package breakpoints;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.breakpoints.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import java.util.List;

/**
 * Created by rishajai on 10/1/16.
 */
//public class XBreakpointBase<Self extends XBreakpoint<P>, P extends XBreakpointProperties, S extends BreakpointState> extends UserDataHolderBase implements XBreakpoint<P>, Comparable<Self> {

//public class XLineBreakpointImpl<P extends XBreakpointProperties> extends com.intellij.xdebugger.impl.breakpoints.XBreakpointBase<XLineBreakpoint<P>, P, LineBreakpointState<P>> implements XLineBreakpoint<P> {

public class JavaProfilerLineBreakpointType extends JavaBreakpointTypeBase<JavaProfilerLineBreakpointProperties>
        implements JavaBreakpointType<JavaProfilerLineBreakpointProperties> {

    public JavaProfilerLineBreakpointType() {
        super("java-profiler-line", "Profiler Line Breakpoints");
    }

    protected JavaProfilerLineBreakpointType(@NonNls @NotNull String id, @Nls @NotNull String title) {
        super(id, title);
    }

    @Override
    public String getDisplayText(XBreakpoint xBreakpoint) {
        return "some text...";
    }

    protected String getHelpID() {
        return "debugging.profilerLineBreakpoint";
    }

    public String getDisplayName() {
        return "Profiler line breakpoints...";
    }

    @NotNull
    public Breakpoint<JavaProfilerLineBreakpointProperties> createJavaBreakpoint(Project project, XBreakpoint<JavaProfilerLineBreakpointProperties>
            breakpoint) {
        JavaProfilerLineBreakpointProperties properties = breakpoint.getProperties();
        return new ProfilerLineBreakpoint(project, properties.getVirtualFile(), properties.getLine(), breakpoint);
        //super.getSour
    }

    @Nullable
    public PsiElement getContainingMethod(@NotNull ProfilerLineBreakpoint breakpoint) {
        SourcePosition position = breakpoint.getSourcePosition();
        if(position == null) {
            return null;
        } else {
            JavaBreakpointProperties properties = breakpoint.getXBreakpoint().getProperties();
            Integer ordinal = ((JavaLineBreakpointProperties)properties).getLambdaOrdinal();
            if(ordinal.intValue() > -1) {
                List lambdas = DebuggerUtilsEx.collectLambdas(position, true);
                if(ordinal.intValue() < lambdas.size()) {
                    return (PsiElement)lambdas.get(ordinal.intValue());
                }
            }
            return DebuggerUtilsEx.getContainingMethod(position);
        }
    }

    public boolean matchesPosition(@NotNull ProfilerLineBreakpoint breakpoint, @NotNull SourcePosition position) {
        JavaProfilerLineBreakpointProperties properties = breakpoint.getXBreakpoint().getProperties();
        if(((JavaProfilerLineBreakpointProperties)properties).getLambdaOrdinal() == null) {
            return true;
        } else {
            PsiElement containingMethod = this.getContainingMethod(breakpoint);
            return containingMethod == null?false:DebuggerUtilsEx.inTheMethod(position, containingMethod);
        }
    }

    @Nullable
    public XSourcePosition getSourcePosition(@NotNull XBreakpoint<JavaProfilerLineBreakpointProperties> xBreakpoint) {
        JavaProfilerLineBreakpointProperties properties = xBreakpoint.getProperties();
        if (properties.getXSourcePosition() == null) {
            XSourcePosition sourcePosition = null;
            VirtualFile file = properties.getVirtualFile();
            int line = properties.getLine();
            properties.setXSourcePosition(XSourcePositionImpl.create(file, line));
        }
        return properties.getXSourcePosition();
    }

//    @Override
//    public XSourcePosition getSourcePosition(@NotNull XBreakpoint<JavaLineBreakpointProperties> xBreakpoint) {
//        ProfilerLineBreakpoint lineBreakpoint = (ProfilerLineBreakpoint) BreakpointManager.getJavaBreakpoint
//                (xBreakpoint);
//        XSourcePosition xSourcePosition = lineBreakpoint.getXSourcePosition();
//        if (xSourcePosition != null) {
//            return xSourcePosition;
//        }
//        (new ReadAction() {
//            protected void run(@NotNull Result result) {
//                XSourcePosition xSourcePosition = XDebuggerUtil.getInstance().createPosition(XLineBreakpointImpl.this.getFile(),
//                        XLineBreakpointImpl.this.getLine());
//            }
//        }).execute();
//    }
}
