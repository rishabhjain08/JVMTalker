package breakpoints;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.xdebugger.XSourcePosition;
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties;

/**
 * Created by rishajai on 10/1/16.
 */
public class JavaProfilerLineBreakpointProperties extends JavaBreakpointProperties<JavaProfilerLineBreakpointProperties> {
    private Integer myLambdaOrdinal = null;
    private VirtualFile file;
    int line;
    private XSourcePosition xSourcePosition;

    public JavaProfilerLineBreakpointProperties(VirtualFile file, int line) {
        this.file = file;
        this.line = line;
    }

    public VirtualFile getVirtualFile() {
        return this.file;
    }

    public int getLine() {
        return this.line;
    }

    public void setXSourcePosition(XSourcePosition xSourcePosition) {
        this.xSourcePosition = xSourcePosition;
    }

    public XSourcePosition getXSourcePosition() {
        return this.xSourcePosition;
    }

    @OptionTag("lambda-ordinal")
    public Integer getLambdaOrdinal() {
        return this.myLambdaOrdinal;
    }

    public void setLambdaOrdinal(Integer lambdaOrdinal) {
        this.myLambdaOrdinal = lambdaOrdinal;
    }

    public void loadState(JavaProfilerLineBreakpointProperties state) {
        super.loadState(state);
        this.myLambdaOrdinal = state.myLambdaOrdinal;
    }
}
