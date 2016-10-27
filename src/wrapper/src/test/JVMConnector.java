package test;

import api.JVMTalkerSession;
import api.ProfilerLineBreakpointCallback;
import breakpoints.ProfilerLineBreakpoint;
import com.intellij.execution.ExecutionException;
import junit.framework.Test;

import java.io.File;
import java.net.URL;

/**
 * Created by rishajai on 10/17/16.
 */
public class JVMConnector {

    private String host;
    private int port;
    private JVMTalkerSession session;

    public JVMConnector(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private boolean isConnected() {
        return this.session != null;
    }

    public void connect() {
        try {
            this.session = JVMTalkerSession.remoteConnect(host, port + "", false, null);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

//    private String getFileLocation() {
//        URL location = Test.class.getProtectionDomain().getCodeSource().getLocation();
//        return location.getFile();
//    }

    private String getParentDir() {
        return "/Volumes/Unix/workspace/Personal/src/RishajaiRepo/projects/JVMTalker/src/wrapper/src/test";
//        String path = this.getFileLocation();
//        return path.substring(0, path.lastIndexOf(File.separator));
    }

    private ProfilerLineBreakpointCallback cb = new ProfilerLineBreakpointCallback() {

        private long firstHitAt = 0L;

        @Override
        public void breakpointHit(ProfilerLineBreakpoint breakpoint) {
            if (firstHitAt == 0L) {
                firstHitAt = System.currentTimeMillis();
            }
            System.out.printf("[EXECUTED] " + breakpoint.getPsiFile().getName() + ":" + breakpoint.getLineIndex() + " "
                    + "%10.1f seconds elapsed...\n", ((System.currentTimeMillis() - firstHitAt) / (1000.0)));
        }
    };

    private void addBreakpoint(String path, int line, ProfilerLineBreakpointCallback cb) {
        session.addBreakpoint(path, line, cb);
        System.out.println("adding breakpoint for " + path + ":" + line);
    }

    public void beginTesting() {
        assert isConnected();
        System.out.println("Connected to the JVM... :)");
        String pathToTempClass = getParentDir() + File.separator + "MainRunner.java";
        addBreakpoint(pathToTempClass, 28, cb);
        addBreakpoint(pathToTempClass, 29, cb);
        addBreakpoint(pathToTempClass, 30, cb);
//        addBreakpoint(pathToTempClass, 23, cb);
//        addBreakpoint(pathToTempClass, 31, cb);
//        addBreakpoint(pathToTempClass, 33, cb);
//        addBreakpoint(pathToTempClass, 34, cb);
        while(true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main (String[] args) throws InterruptedException {
        JVMConnector connector = new JVMConnector("localhost", 5009);
        System.out.println("connecting...");
        connector.connect();
        System.out.println("connected...");
        connector.beginTesting();
    }
}
