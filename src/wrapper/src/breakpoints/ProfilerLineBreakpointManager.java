package breakpoints;

import api.ProfilerLineBreakpointCallback;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.HashMap;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.impl.breakpoints.LineBreakpointState;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointManagerImpl;
import com.intellij.xdebugger.impl.breakpoints.XDependentBreakpointManager;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rishajai on 10/2/16.
 */
public class ProfilerLineBreakpointManager {

    private static ProfilerLineBreakpointManager manager;
    private static long ProfileLineBreakpointCounter = 0;
    private Project project;
    private ProfilerLineBreakpointManager(Project project) {
        this.project = project;
    }

    public synchronized static ProfilerLineBreakpointManager getProfilerLineBreakpointManager(Project project) {
        if (manager != null) {
            return manager;
        }
        manager = new ProfilerLineBreakpointManager(project);
        return manager;
    }


    public void removeAllProfileLineBreakpoints() {
        BreakpointManager breakpointManager = DebuggerManagerEx.getInstanceEx(project).getBreakpointManager();
        XDependentBreakpointManager dependentBreakpointManager = ((XBreakpointManagerImpl) XDebuggerManager.getInstance
                (project).getBreakpointManager()).getDependentBreakpointManager();
        List<Breakpoint> allBreakpoints = breakpointManager.getBreakpoints();
        ProfilerLineBreakpoint profilerLineBreakpoint = null;
        boolean profileBreakpointFound;
        //System.out.println("_____________________________________");
        for (Breakpoint breakpoint : allBreakpoints) {
            if (breakpoint instanceof ProfilerLineBreakpoint) {
                removeProfilerLineBreakpoint((ProfilerLineBreakpoint) breakpoint);
            }
        }
    }

    public void removeProfilerLineBreakpoint(ProfilerLineBreakpoint profilerLineBreakpoint) {
        BreakpointManager breakpointManager = DebuggerManagerEx.getInstanceEx(project).getBreakpointManager();
        if (profilerLineBreakpoint != null) {
            (new WriteAction() {
                protected void run(Result result) {
                    breakpointManager.removeBreakpoint(profilerLineBreakpoint);
                }
            }).execute();
        }
    }

    public ProfilerLineBreakpoint addProfilerLineBreakpoint(VirtualFile file, int line, ProfilerLineBreakpointCallback cb) {
        ProfilerLineBreakpoint profilerLineBreakpoint = addProfilerLineBreakpoint(file, line);
        if (profilerLineBreakpoint != null) {
            profilerLineBreakpoint.addProfilerLineBreakpointCallback(cb);
        }
        return profilerLineBreakpoint;
    }

    private ProfilerLineBreakpoint addProfilerLineBreakpoint(VirtualFile file, int line) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        XBreakpointBase xBreakpoint = addXBreakpoint(JavaProfilerLineBreakpointType.class, project, file, line);
        Breakpoint breakpoint = BreakpointManager.getJavaBreakpoint(xBreakpoint);
        if(breakpoint instanceof ProfilerLineBreakpoint) {
            BreakpointManager.addBreakpoint(breakpoint);
            return (ProfilerLineBreakpoint)breakpoint;
        } else {
            return null;
        }
    }

    private <B extends XBreakpoint<?>> XBreakpointBase addXBreakpoint(Class<? extends XBreakpointType<B, ?>> typeCls,
                                                                      Project project, VirtualFile file, int
                                                                              lineIndex) {
        XBreakpointManagerImpl xBreakpointManager = (XBreakpointManagerImpl) XDebuggerManager.getInstance(project).getBreakpointManager();
        XBreakpointType type = new JavaProfilerLineBreakpointType();
        return (XBreakpointBase)ApplicationManager.getApplication().<XBreakpointBase>runWriteAction(() -> {
            XBreakpointBase xBreakpointBase = (XBreakpointBase) (xBreakpointManager.addBreakpoint(type, new
                    JavaProfilerLineBreakpointProperties(file, lineIndex)));
//            LineBreakpointState state = new LineBreakpointState(true, type.getId(), file.getUrl(), lineIndex, false,
//                    ProfileLineBreakpointCounter++);
            return xBreakpointBase;
        });
    }
}
