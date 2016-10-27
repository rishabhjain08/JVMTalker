package breakpoints;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaBreakpointHandler;
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory;

/**
 * Created by rishajai on 10/2/16.
 */

public class JavaProfilerLineBreakpointHandlerFactory implements JavaBreakpointHandlerFactory {

    @Override
    public JavaBreakpointHandler createHandler(DebugProcessImpl debugProcess) {
        return new JavaProfileLineBreakpointHandler(debugProcess);
    }

    public static class JavaProfileLineBreakpointHandler extends JavaBreakpointHandler {
        public JavaProfileLineBreakpointHandler(DebugProcessImpl process) {
            super(JavaProfilerLineBreakpointType.class, process);
        }
    }
}