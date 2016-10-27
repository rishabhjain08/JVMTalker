package api;

import breakpoints.JavaProfilerLineBreakpointHandlerFactory;
import breakpoints.ProfilerLineBreakpoint;
import com.intellij.debugger.DebugEnvironment;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.DefaultDebugEnvironment;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.engine.RemoteDebugProcessHandler;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.mock.MockFileDocumentManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl;
import com.intellij.profile.ProfileManager;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.*;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.XDebuggerManagerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rishajai on 10/15/16.
 */
public class JVMTalkerSession {

    private static final Logger LOG = Logger.getInstance(JVMTalkerSession.class);
    private Map<String, ProfilerLineBreakpoint> breakpointMap;
    private Project myProject;

    private DebuggerSession debuggerSession;
    private DebugEnvironment debugEnvironment;
    private JavaProfilerLineBreakpointHandlerFactory.JavaProfileLineBreakpointHandler breakpointHandler;
    private static final JavaProfilerLineBreakpointHandlerFactory handlerFactory =
            new JavaProfilerLineBreakpointHandlerFactory();
    private JVMTalkerSessionStatusCallback statusCallback;

    private JVMTalkerSession(Project project, DebuggerSession debuggerSession,
                DebugEnvironment debugEnvironment, JVMTalkerSessionStatusCallback statusCallback) {
        this.myProject = project;
        this.debuggerSession = debuggerSession;
        this.debugEnvironment = debugEnvironment;
        this.statusCallback = statusCallback;
        breakpointHandler = (JavaProfilerLineBreakpointHandlerFactory.JavaProfileLineBreakpointHandler) handlerFactory
                .createHandler(debuggerSession.getProcess());
        breakpointMap = new HashMap<>();
    }

    public static JVMTalkerSession remoteConnect(String hostName, String address, boolean serverMode,
                                                 JVMTalkerSessionStatusCallback statusCallback)
            throws ExecutionException {
        if (!ProfilerManager.isInitialized()) {
            try {
                ProfilerManager.init();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        final String sessionName = new Random().nextLong() + "";
        final ProcessHandler processHandler = new RemoteDebugProcessHandler(ProfilerManager.getProject());
        DebugEnvironment environment = new DebugEnvironment() {

            @Override
            public ExecutionResult createExecutionResult() throws ExecutionException {
                processHandler.addProcessListener(new ProcessListener() {
                    @Override
                    public void startNotified(ProcessEvent processEvent) {

                    }

                    @Override
                    public void processTerminated(ProcessEvent processEvent) {

                    }

                    @Override
                    public void processWillTerminate(ProcessEvent processEvent, boolean b) {

                    }

                    @Override
                    public void onTextAvailable(ProcessEvent processEvent, Key key) {
                        LOG.info(processEvent.getText());
                    }
                });
                return new DefaultExecutionResult(null, processHandler);
            }

            @Override
            public GlobalSearchScope getSearchScope() {
                return new MySearchScope();
            }

            @Override
            public boolean isRemote() {
                return true;
            }

            @Override
            public RemoteConnection getRemoteConnection() {
                return new RemoteConnection(true, hostName, address, serverMode);
            }

            @Override
            public long getPollTimeout() {
                return 1;
            }

            @Override
            public String getSessionName() {
                return sessionName;
            }
        };
        DebuggerManagerEx debuggerManagerEx = DebuggerManagerEx.getInstanceEx(ProfilerManager.getProject());
        DebuggerSession debuggerSession = debuggerManagerEx.attachVirtualMachine(environment);
        processHandler.startNotify();
        JVMTalkerSession jvmTalkerSession = new JVMTalkerSession(ProfilerManager.getProject(), debuggerSession, environment, statusCallback);
        return jvmTalkerSession;
    }

    public synchronized boolean addBreakpoint(String path, int line, ProfilerLineBreakpointCallback callback) {
        VirtualFileManagerImpl virtualFileManager = (VirtualFileManagerImpl) ApplicationManager.getApplication()
                .getComponent(VirtualFileManager.class);
        VirtualFile file = virtualFileManager.findFileByUrl("file://" + path);
        ProfilerLineBreakpoint profilerLineBreakpoint = ProfilerManager.getProfilerLineBreakpointManager()
                .addProfilerLineBreakpoint(file, line, callback);
        debuggerSession.getProcess().registerFile(file);
        ((MySearchScope) debugEnvironment.getSearchScope()).addFile(file);
        MyCoreJavaFileManager coreJavaFileManager = ((MyProjectImpl) ProfilerManager.getProject())
                .getCoreJavaFileManager();
        VirtualFile parentFile = file.getParent();
        if (parentFile != null) {
            coreJavaFileManager.addToClasspath(parentFile);
        }
        breakpointMap.put(path + ":" + line, profilerLineBreakpoint);
        breakpointHandler.registerBreakpoint(profilerLineBreakpoint.getXBreakpoint());
        return profilerLineBreakpoint != null;
    }

    public synchronized void removeBreakpoint(String path, int line) {
        ProfilerLineBreakpoint profilerLineBreakpoint = breakpointMap.get(path + ":" + line);
        if (profilerLineBreakpoint != null) {
            breakpointMap.remove(path + ":" + line);
            breakpointHandler.unregisterBreakpoint(profilerLineBreakpoint.getXBreakpoint(), false);
        }
    }
}
