package breakpoints;

import api.ProfilerLineBreakpointCallback;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.*;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.requests.Requestor;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointCategory;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.BreakpointRequest;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by rishajai on 10/1/16.
 */
public class ProfilerLineBreakpoint extends Breakpoint<JavaProfilerLineBreakpointProperties> {

    private static final Logger LOG = Logger.getInstance("#breakpoints.ProfilerLineBreakpoint");
    @NonNls
    public static final Key<LineBreakpoint> CATEGORY = BreakpointCategory.lookup("profiler_line_breakpoints");
    private List<ProfilerLineBreakpointCallback> breakpointHitCallbacks;
    private static final Icon myIcon = null;//AllIcons.Debugger.Db_muted_dep_line_breakpoint;
    private static final Pattern ourAnonymousPattern = Pattern.compile(".*\\$\\d*$");
    private final XBreakpoint myXBreakpoint;
    private SourcePosition mySourcePosition;
    private VirtualFile file;
    private int line;

    protected ProfilerLineBreakpoint(Project project, VirtualFile file, int line, XBreakpoint xBreakpoint) {
        super(project, xBreakpoint);
        this.myXBreakpoint = xBreakpoint;
        this.file = file;
        this.line = line;
        breakpointHitCallbacks = new LinkedList<>();
    }

    //    @Override
//    public boolean isVisible() {
//        return false;
//    }


    @Nullable
    public PsiFile getPsiFile() {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        XSourcePosition position = this.myXBreakpoint.getSourcePosition();
        if(position != null) {
            VirtualFile file = position.getFile();
            if(file.isValid()) {
                return PsiManager.getInstance(this.myProject).findFile(file);
            }
        }

        return null;
    }

    public SourcePosition getSourcePosition() {
        if (mySourcePosition != null) {
            return mySourcePosition;
        }
        PsiManagerImpl psiManager = (PsiManagerImpl) ApplicationManager.getApplication().getComponent(PsiManager.class);
        PsiFile psiFile = psiManager.findFile(file);
        mySourcePosition = SourcePosition.createFromLine(psiFile, line);
        return mySourcePosition;
    }

    protected static PsiClass getPsiClassAt(@Nullable final SourcePosition sourcePosition) {
        return (PsiClass)ApplicationManager.getApplication().runReadAction(new Computable() {
            @Nullable
            public PsiClass compute() {
                return JVMNameUtil.getClassAt(sourcePosition);
            }
        });
    }

    public PsiClass getPsiClass() {
        SourcePosition sourcePosition = this.getSourcePosition();
        return getPsiClassAt(sourcePosition);
    }

    public void createRequest(@NotNull DebugProcessImpl debugProcess) {
        DebuggerManagerThreadImpl.assertIsManagerThread();
        if(this.shouldCreateRequest(debugProcess)) {
            if(this.isValid()) {
                SourcePosition position = this.getSourcePosition();
                if(position != null) {
                    this.createOrWaitPrepare(debugProcess, position);
                } else {
                    LOG.error("Unable to create request for breakpoint with null position: " + this.toString() + " at" +
                            " " + myXBreakpoint.getSourcePosition());
                }
            }
        }
    }

    @Nullable
    private Collection<VirtualFile> findClassCandidatesInSourceContent(String className, final GlobalSearchScope scope, final ProjectFileIndex fileIndex) {
        int dollarIndex = className.indexOf("$");
        final String topLevelClassName = dollarIndex >= 0?className.substring(0, dollarIndex):className;
        return (Collection)ApplicationManager.getApplication().runReadAction(new Computable() {
            @Nullable
            public Collection<VirtualFile> compute() {
                PsiClass[] classes = JavaPsiFacade.getInstance(myProject).findClasses(topLevelClassName, scope);
                if(ProfilerLineBreakpoint.LOG.isDebugEnabled()) {
                    ProfilerLineBreakpoint.LOG.debug("Found " + classes.length + " classes " + topLevelClassName + " in scope " + scope);
                }

                if(classes.length == 0) {
                    return null;
                } else {
                    ArrayList list = new ArrayList(classes.length);
                    PsiClass[] var3 = classes;
                    int var4 = classes.length;

                    for(int var5 = 0; var5 < var4; ++var5) {
                        PsiClass aClass = var3[var5];
                        PsiFile psiFile = aClass.getContainingFile();
                        if(ProfilerLineBreakpoint.LOG.isDebugEnabled()) {
                            StringBuilder vFile = new StringBuilder();
                            vFile.append("Checking class ").append(aClass.getQualifiedName());
                            vFile.append("\n\t").append("PsiFile=").append(psiFile);
                            if(psiFile != null) {
                                VirtualFile vFile1 = psiFile.getVirtualFile();
                                vFile.append("\n\t").append("VirtualFile=").append(vFile1);
                                if(vFile1 != null) {
                                    vFile.append("\n\t").append("isInSourceContent=").append(fileIndex.isUnderSourceRootOfType(vFile1, JavaModuleSourceRootTypes.SOURCES));
                                }
                            }

                            ProfilerLineBreakpoint.LOG.debug(vFile.toString());
                        }

                        if(psiFile == null) {
                            return null;
                        }

                        VirtualFile var10 = psiFile.getVirtualFile();
                        if(var10 == null || !fileIndex.isUnderSourceRootOfType(var10, JavaModuleSourceRootTypes.SOURCES)) {
                            return null;
                        }

                        list.add(var10);
                    }

                    return list;
                }
            }
        });
    }

    private boolean isInScopeOf(DebugProcessImpl debugProcess, String className) {
        SourcePosition position = this.getSourcePosition();
        if(position != null) {
            VirtualFile breakpointFile = position.getFile().getVirtualFile();
            ProjectFileIndex fileIndex = ProjectRootManager.getInstance(this.myProject).getFileIndex();
            if(breakpointFile != null && fileIndex.isUnderSourceRootOfType(breakpointFile, JavaModuleSourceRootTypes.SOURCES)) {
                if(debugProcess.getSearchScope().contains(breakpointFile)) {
                    return true;
                }

                Collection candidates = this.findClassCandidatesInSourceContent(className, debugProcess.getSearchScope(), fileIndex);
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Found " + (candidates == null?"null":Integer.valueOf(candidates.size())) + " candidate containing files for class " + className);
                }

                if(candidates == null) {
                    return true;
                }

                if(LOG.isDebugEnabled()) {
                    GlobalSearchScope scope = debugProcess.getSearchScope();
                    boolean contains = scope.contains(breakpointFile);
                    Project project = this.getProject();
                    List files = ContainerUtil.map(JavaFullClassNameIndex.getInstance().get(Integer.valueOf(className.hashCode()), project, scope), (aClass) -> {
                        return aClass.getContainingFile().getVirtualFile();
                    });
                    List allFiles = ContainerUtil.map(JavaFullClassNameIndex.getInstance().get(Integer.valueOf(className.hashCode()), project, new EverythingGlobalScope(project)), (aClass) -> {
                        return aClass.getContainingFile().getVirtualFile();
                    });
                    VirtualFile contentRoot = fileIndex.getContentRootForFile(breakpointFile);
                    Module module = fileIndex.getModuleForFile(breakpointFile);
                    LOG.debug("Did not find \'" + className + "\' in " + scope + "; contains=" + contains + "; contentRoot=" + contentRoot + "; module = " + module + "; all files in index are: " + files + "; all possible files are: " + allFiles);
                }

                return false;
            }
        }

        return true;
    }

    private static boolean isAnonymousClass(ReferenceType classType) {
        return classType instanceof ClassType?ourAnonymousPattern.matcher(classType.name()).matches():false;
    }

    @Nullable
    protected JavaProfilerLineBreakpointType getXBreakpointType() {
        XBreakpointType type = this.myXBreakpoint.getType();
        return type instanceof JavaProfilerLineBreakpointType?(JavaProfilerLineBreakpointType)type:null;
    }

    protected boolean acceptLocation(DebugProcessImpl debugProcess, ReferenceType classType, Location loc) {
        Method method = loc.method();
        return !isAnonymousClass(classType) || (!method.isConstructor() || loc.codeIndex() != 0L) && !method.isBridge
                ()?((Boolean)ApplicationManager.getApplication().<Boolean>runReadAction(() -> {
            SourcePosition position = debugProcess.getPositionManager().getSourcePosition(loc, getPsiFile());
            if(position == null) {
                return Boolean.valueOf(false);
            } else {
                JavaProfilerLineBreakpointType type = this.getXBreakpointType();
                return type == null?Boolean.valueOf(true):Boolean.valueOf(type.matchesPosition(this, position));
            }
        })).booleanValue():false;
    }

    public int getLineIndex() {
        XSourcePosition sourcePosition = this.myXBreakpoint.getSourcePosition();
        return sourcePosition != null?sourcePosition.getLine():-1;
    }

    protected String getFileName() {
        XSourcePosition sourcePosition = this.myXBreakpoint.getSourcePosition();
        return sourcePosition != null?sourcePosition.getFile().getName():"";
    }

    protected void createRequestForPreparedClass(DebugProcessImpl debugProcess, ReferenceType classType) {
        if(!this.isInScopeOf(debugProcess, classType.name())) {
            if(LOG.isDebugEnabled()) {
                LOG.debug(classType.name() + " is out of debug-process scope, breakpoint request won\'t be created for line " + this.getLineIndex());
            }

        } else {
            try {
                List ex = debugProcess.getPositionManager().locationsOfLine(classType, this.getSourcePosition());
                if(!ex.isEmpty()) {
                    Iterator var4 = ex.iterator();

                    while(var4.hasNext()) {
                        Location loc = (Location)var4.next();
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Found location [codeIndex=" + loc.codeIndex() + "] for reference type " + classType.name() + " at line " + this.getLineIndex() + "; isObsolete: " + (debugProcess.getVirtualMachineProxy().versionHigher("1.4") && loc.method().isObsolete()));
                        }

                        boolean acceptLocation = this.acceptLocation(debugProcess, classType, loc);
                        if(acceptLocation) {
                            BreakpointRequest request = debugProcess.getRequestsManager().createBreakpointRequest(this, loc);
                            debugProcess.getRequestsManager().enableRequest(request);
                            if(LOG.isDebugEnabled()) {
                                LOG.debug("Created breakpoint request for reference type " + classType.name() + " at line " + this.getLineIndex() + "; codeIndex=" + loc.codeIndex());
                            }
                        }
                    }
                } else {
                    debugProcess.getRequestsManager().setInvalid(this, DebuggerBundle.message("error.invalid.breakpoint.no.executable.code", new Object[]{Integer.valueOf(this.getLineIndex() + 1), classType.name()}));
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("No locations of type " + classType.name() + " found at line " + this.getLineIndex());
                    }
                }
            } catch (ClassNotPreparedException var7) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("ClassNotPreparedException: " + var7.getMessage());
                }
            } catch (ObjectCollectedException var8) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("ObjectCollectedException: " + var8.getMessage());
                }
            } catch (InvalidLineNumberException var9) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("InvalidLineNumberException: " + var9.getMessage());
                }

                debugProcess.getRequestsManager().setInvalid(this, DebuggerBundle.message("error.invalid.breakpoint.bad.line.number", new Object[0]));
            } catch (Exception var10) {
                LOG.info(var10);
            }

            this.updateUI();
        }
    }

    @Override
    public void processClassPrepare(DebugProcess debugProcess, ReferenceType classType) {
        if(this.isEnabled() && this.isValid()) {
            this.createRequestForPreparedClass((DebugProcessImpl)debugProcess, classType);
            this.updateUI();
        }
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public void reload() {

    }

    @Override
    public String getEventMessage(LocatableEvent locatableEvent) {
        return null;
    }

    protected static boolean isPositionValid(@Nullable final XSourcePosition sourcePosition) {
        return ((Boolean)ApplicationManager.getApplication().runReadAction(new Computable() {
            public Boolean compute() {
                LOG.debug("sourceposition not null " + (sourcePosition != null));
//                + " -- file valid " + sourcePosition
//                        .getFile().isValid());
                return Boolean.valueOf(sourcePosition != null && sourcePosition.getFile().isValid());
            }
        })).booleanValue();
    }

    @Override
    public boolean isValid() {
        LOG.debug("in isValid " + this.myXBreakpoint.getSourcePosition());
        return isPositionValid(this.myXBreakpoint.getSourcePosition());
    }

    @Override
    public Key<? extends Breakpoint> getCategory() {
        return null;
    }

    @Override
    public boolean processLocatableEvent(SuspendContextCommandImpl action, LocatableEvent event) throws EventProcessingException {
        DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx(myProject).getContext().getDebuggerSession();
        RequestManagerImpl requestManager = debuggerSession.getProcess().getRequestsManager();
        Requestor requestor = requestManager.findRequestor(event.request());
        //System.out.println("process locatable event... enabled - " + event.request().isEnabled() + " request name -
        // " +
          //      event.request().getClass().getName());
        boolean requestHit = super.processLocatableEvent(action, event);
        if (requestHit) {
            for (ProfilerLineBreakpointCallback cb : breakpointHitCallbacks) {
                cb.breakpointHit((ProfilerLineBreakpoint) requestor);
            }
        }
        return requestHit;
    }

    @Nullable
    @Override
    public PsiElement getEvaluationElement() {
        return null;
    }

    @Override
    public String getSuspendPolicy() {
        return "SuspendNone";
    }

    public void addProfilerLineBreakpointCallback(ProfilerLineBreakpointCallback cb) {
        if (cb != null) {
            this.breakpointHitCallbacks.add(cb);
        }
    }
}
