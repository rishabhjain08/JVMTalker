//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intellij.debugger.engine;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory;
import com.intellij.debugger.engine.JavaExecutionStack;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.JavaValueMarker;
import com.intellij.debugger.engine.MethodFilter;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.JavaBreakpointHandler.JavaExceptionBreakpointHandler;
import com.intellij.debugger.engine.JavaBreakpointHandler.JavaFieldBreakpointHandler;
import com.intellij.debugger.engine.JavaBreakpointHandler.JavaLineBreakpointHandler;
import com.intellij.debugger.engine.JavaBreakpointHandler.JavaMethodBreakpointHandler;
import com.intellij.debugger.engine.JavaBreakpointHandler.JavaWildcardBreakpointHandler;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerContextListener;
import com.intellij.debugger.impl.DebuggerContextUtil;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.DebuggerStateManager;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.impl.SourceCodeChecker;
import com.intellij.debugger.impl.DebuggerSession.Event;
import com.intellij.debugger.impl.PrioritizedTask.Priority;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.debugger.ui.AlternativeSourceNotificationProvider;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.impl.ThreadsPanel;
import com.intellij.debugger.ui.impl.watch.DebuggerTree;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.debugger.ui.impl.watch.MessageDescriptor;
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.ExecutionConsoleEx;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons.Debugger;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Anchor;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionAdapter;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.frame.XValueMarkerProvider;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.XDebuggerUtilImpl;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import com.sun.jdi.event.LocatableEvent;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.JavaDebuggerEditorsProvider;

public class JavaDebugProcess extends XDebugProcess {
  private final DebuggerSession myJavaSession;
  private final JavaDebuggerEditorsProvider myEditorsProvider;
  private final XBreakpointHandler<?>[] myBreakpointHandlers;
  private final NodeManagerImpl myNodeManager;

  public static JavaDebugProcess create(@NotNull XDebugSession session, DebuggerSession javaSession) {
    JavaDebugProcess res = new JavaDebugProcess(session, javaSession);
    javaSession.getProcess().setXDebugProcess(res);
    return res;
  }

  protected JavaDebugProcess(@NotNull final XDebugSession session, final DebuggerSession javaSession) {
    super(session);
    this.myJavaSession = javaSession;
    this.myEditorsProvider = new JavaDebuggerEditorsProvider();
    final DebugProcessImpl process = javaSession.getProcess();
    ArrayList handlers = new ArrayList();
    handlers.add(new JavaLineBreakpointHandler(process));
    handlers.add(new JavaExceptionBreakpointHandler(process));
    handlers.add(new JavaFieldBreakpointHandler(process));
    handlers.add(new JavaMethodBreakpointHandler(process));
    handlers.add(new JavaWildcardBreakpointHandler(process));
    JavaBreakpointHandlerFactory[] var5 = (JavaBreakpointHandlerFactory[])Extensions.getExtensions(JavaBreakpointHandlerFactory.EP_NAME);
    int var6 = var5.length;

    for(int var7 = 0; var7 < var6; ++var7) {
      JavaBreakpointHandlerFactory factory = var5[var7];
      handlers.add(factory.createHandler(process));
    }

    this.myBreakpointHandlers = (XBreakpointHandler[])handlers.toArray(new XBreakpointHandler[handlers.size()]);
    this.myJavaSession.getContextManager().addListener(new DebuggerContextListener() {
      public void changeEvent(@NotNull final DebuggerContextImpl newContext, Event event) {
        if(event == Event.PAUSE || event == Event.CONTEXT || event == Event.REFRESH || event == Event.REFRESH_WITH_STACK && JavaDebugProcess.this.myJavaSession.isPaused()) {
          final SuspendContextImpl newSuspendContext = newContext.getSuspendContext();
          if(newSuspendContext != null && (JavaDebugProcess.this.shouldApplyContext(newContext) || event == Event.REFRESH_WITH_STACK)) {
            process.getManagerThread().schedule(new SuspendContextCommandImpl(newSuspendContext) {
              public void contextAction() throws Exception {
                ThreadReferenceProxyImpl threadProxy = newContext.getThreadProxy();
                newSuspendContext.initExecutionStacks(threadProxy);
                Pair item = (Pair)ContainerUtil.getFirstItem(DebuggerUtilsEx.getEventDescriptors(newSuspendContext));
                if(item != null) {
                  XBreakpoint xBreakpoint = ((Breakpoint)item.getFirst()).getXBreakpoint();
                  com.sun.jdi.event.Event second = (com.sun.jdi.event.Event)item.getSecond();
                  if(xBreakpoint != null && second instanceof LocatableEvent && threadProxy != null && ((LocatableEvent)second).thread() == threadProxy.getThreadReference()) {
                    ((XDebugSessionImpl)JavaDebugProcess.this.getSession()).breakpointReachedNoProcessing(xBreakpoint, newSuspendContext);
                    JavaDebugProcess.this.unsetPausedIfNeeded(newContext);
                    SourceCodeChecker.checkSource(newContext);
                    return;
                  }
                }

                JavaDebugProcess.this.getSession().positionReached(newSuspendContext);
                JavaDebugProcess.this.unsetPausedIfNeeded(newContext);
                SourceCodeChecker.checkSource(newContext);
              }
            });
          }
        } else if(event == Event.ATTACHED) {
          JavaDebugProcess.this.getSession().rebuildViews();
        }

      }
    });
    this.myNodeManager = new NodeManagerImpl(session.getProject(), (DebuggerTree)null) {
      public DebuggerTreeNodeImpl createNode(NodeDescriptor descriptor, EvaluationContext evaluationContext) {
        return new DebuggerTreeNodeImpl((DebuggerTree)null, descriptor);
      }

      public DebuggerTreeNodeImpl createMessageNode(MessageDescriptor descriptor) {
        return new DebuggerTreeNodeImpl((DebuggerTree)null, descriptor);
      }

      public DebuggerTreeNodeImpl createMessageNode(String message) {
        return new DebuggerTreeNodeImpl((DebuggerTree)null, new MessageDescriptor(message));
      }
    };
    session.addSessionListener(new XDebugSessionAdapter() {
      public void sessionPaused() {
        JavaDebugProcess.this.saveNodeHistory();
        this.showAlternativeNotification(session.getCurrentStackFrame());
      }

      public void stackFrameChanged() {
        XStackFrame frame = session.getCurrentStackFrame();
        if(frame instanceof JavaStackFrame) {
          this.showAlternativeNotification(frame);
          StackFrameProxyImpl frameProxy = ((JavaStackFrame)frame).getStackFrameProxy();
          DebuggerContextUtil.setStackFrame(javaSession.getContextManager(), frameProxy);
          JavaDebugProcess.this.saveNodeHistory(frameProxy);
        }

      }

      private void showAlternativeNotification(@Nullable XStackFrame frame) {
        if(frame != null) {
          XSourcePosition position = frame.getSourcePosition();
          if(position != null) {
            VirtualFile file = position.getFile();
            if(!AlternativeSourceNotificationProvider.fileProcessed(file)) {
              EditorNotifications.getInstance(session.getProject()).updateNotifications(file);
            }
          }
        }

      }
    });
  }

  private void unsetPausedIfNeeded(DebuggerContextImpl context) {
    SuspendContextImpl suspendContext = context.getSuspendContext();
    if(suspendContext != null && !suspendContext.suspends(context.getThreadProxy())) {
      ((XDebugSessionImpl)this.getSession()).unsetPaused();
    }

  }

  private boolean shouldApplyContext(DebuggerContextImpl context) {
    SuspendContextImpl suspendContext = context.getSuspendContext();
    SuspendContextImpl currentContext = (SuspendContextImpl)this.getSession().getSuspendContext();
    if(suspendContext != null && !suspendContext.equals(currentContext)) {
      return true;
    } else {
      JavaExecutionStack currentExecutionStack = currentContext != null?currentContext.getActiveExecutionStack():null;
      return currentExecutionStack == null || !Comparing.equal(context.getThreadProxy(), currentExecutionStack.getThreadProxy());
    }
  }

  public void saveNodeHistory() {
    this.saveNodeHistory(this.getDebuggerStateManager().getContext().getFrameProxy());
  }

  private void saveNodeHistory(final StackFrameProxyImpl frameProxy) {
    this.myJavaSession.getProcess().getManagerThread().invoke(new DebuggerCommandImpl() {
      protected void action() throws Exception {
        JavaDebugProcess.this.myNodeManager.setHistoryByContext(frameProxy);
      }

      public Priority getPriority() {
        return Priority.NORMAL;
      }
    });
  }

  private DebuggerStateManager getDebuggerStateManager() {
    return this.myJavaSession.getContextManager();
  }

  public DebuggerSession getDebuggerSession() {
    return this.myJavaSession;
  }

  @NotNull
  public XDebuggerEditorsProvider getEditorsProvider() {
    JavaDebuggerEditorsProvider var10000 = this.myEditorsProvider;
    if(this.myEditorsProvider == null) {
      throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/intellij/debugger/engine/JavaDebugProcess", "getEditorsProvider"}));
    } else {
      return var10000;
    }
  }

  public void startStepOver(@Nullable XSuspendContext context) {
    this.myJavaSession.stepOver(false);
  }

  public void startStepInto(@Nullable XSuspendContext context) {
    this.myJavaSession.stepInto(false, (MethodFilter)null);
  }

  public void startForceStepInto(@Nullable XSuspendContext context) {
    this.myJavaSession.stepInto(true, (MethodFilter)null);
  }

  public void startStepOut(@Nullable XSuspendContext context) {
    this.myJavaSession.stepOut();
  }

  public void stop() {
    this.myJavaSession.dispose();
    this.myNodeManager.dispose();
  }

  public void startPausing() {
    this.myJavaSession.pause();
  }

  public void resume(@Nullable XSuspendContext context) {
    this.myJavaSession.resume();
  }

  public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
    this.myJavaSession.runToCursor(position, false);
  }

  @NotNull
  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    XBreakpointHandler[] var10000 = this.myBreakpointHandlers;
    if(this.myBreakpointHandlers == null) {
      throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/intellij/debugger/engine/JavaDebugProcess", "getBreakpointHandlers"}));
    } else {
      return var10000;
    }
  }

  public boolean checkCanInitBreakpoints() {
    return false;
  }

  @Nullable
  protected ProcessHandler doGetProcessHandler() {
    return this.myJavaSession.getProcess().getProcessHandler();
  }

  @NotNull
  public ExecutionConsole createConsole() {
    ExecutionConsole console = this.myJavaSession.getProcess().getExecutionResult().getExecutionConsole();
    return console != null?console:super.createConsole();
  }

  @NotNull
  public XDebugTabLayouter createTabLayouter() {
    return new XDebugTabLayouter() {
      public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
        final ThreadsPanel panel = new ThreadsPanel(JavaDebugProcess.this.myJavaSession.getProject(), JavaDebugProcess.this.getDebuggerStateManager());
        final Content threadsContent = ui.createContent("ThreadsContent", panel, XDebuggerBundle.message("debugger.session.tab.threads.title", new Object[0]), Debugger.Threads, (JComponent)null);
        Disposer.register(threadsContent, panel);
        threadsContent.setCloseable(false);
        ui.addContent(threadsContent, 0, PlaceInGrid.left, true);
        ui.addListener(new ContentManagerAdapter() {
          public void selectionChanged(ContentManagerEvent event) {
            if(event.getContent() == threadsContent) {
              if(threadsContent.isSelected()) {
                panel.setUpdateEnabled(true);
                if(panel.isRefreshNeeded()) {
                  panel.rebuildIfVisible(Event.CONTEXT);
                }
              } else {
                panel.setUpdateEnabled(false);
              }
            }

          }
        }, threadsContent);
      }

      @NotNull
      public Content registerConsoleContent(@NotNull RunnerLayoutUi ui, @NotNull ExecutionConsole console) {
        Content content = null;
        if(console instanceof ExecutionConsoleEx) {
          ((ExecutionConsoleEx)console).buildUi(ui);
          content = ui.findContent("ConsoleContent");
        }

        if(content == null) {
          content = super.registerConsoleContent(ui, console);
        }

        return content;
      }
    };
  }

  public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar, @NotNull DefaultActionGroup topToolbar, @NotNull DefaultActionGroup settings) {
    Constraints beforeRunner = new Constraints(Anchor.BEFORE, "Runner.Layout");
    leftToolbar.add(Separator.getInstance(), beforeRunner);
    leftToolbar.add(ActionManager.getInstance().getAction("DumpThreads"), beforeRunner);
    leftToolbar.add(Separator.getInstance(), beforeRunner);
    Constraints beforeSort = new Constraints(Anchor.BEFORE, "XDebugger.ToggleSortValues");
    settings.addAction(new JavaDebugProcess.WatchLastMethodReturnValueAction(), beforeSort);
    settings.addAction(new JavaDebugProcess.AutoVarsSwitchAction(), beforeSort);
  }

  @Nullable
  private static DebugProcessImpl getCurrentDebugProcess(@Nullable Project project) {
    if(project != null) {
      XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
      if(session != null) {
        XDebugProcess process = session.getDebugProcess();
        if(process instanceof JavaDebugProcess) {
          return ((JavaDebugProcess)process).getDebuggerSession().getProcess();
        }
      }
    }

    return null;
  }

  public NodeManagerImpl getNodeManager() {
    return this.myNodeManager;
  }

  public String getCurrentStateMessage() {
    String description = this.myJavaSession.getStateDescription();
    return description != null?description:super.getCurrentStateMessage();
  }

  @Nullable
  public XValueMarkerProvider<?, ?> createValueMarkerProvider() {
    return new JavaValueMarker();
  }

  public boolean isLibraryFrameFilterSupported() {
    return true;
  }

  private static class WatchLastMethodReturnValueAction extends ToggleAction {
    private volatile boolean myWatchesReturnValues;
    private final String myText;
    private final String myTextUnavailable;

    public WatchLastMethodReturnValueAction() {
      super("", DebuggerBundle.message("action.watch.method.return.value.description", new Object[0]), (Icon)null);
      this.myWatchesReturnValues = DebuggerSettings.getInstance().WATCH_RETURN_VALUES;
      this.myText = DebuggerBundle.message("action.watches.method.return.value.enable", new Object[0]);
      this.myTextUnavailable = DebuggerBundle.message("action.watches.method.return.value.unavailable.reason", new Object[0]);
    }

    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      Presentation presentation = e.getPresentation();
      DebugProcessImpl process = JavaDebugProcess.getCurrentDebugProcess(e.getProject());
      if(process != null && !process.canGetMethodReturnValue()) {
        presentation.setEnabled(false);
        presentation.setText(this.myTextUnavailable);
      } else {
        presentation.setEnabled(true);
        presentation.setText(this.myText);
      }

    }

    public boolean isSelected(AnActionEvent e) {
      return this.myWatchesReturnValues;
    }

    public void setSelected(AnActionEvent e, boolean watch) {
      this.myWatchesReturnValues = watch;
      DebuggerSettings.getInstance().WATCH_RETURN_VALUES = watch;
      DebugProcessImpl process = JavaDebugProcess.getCurrentDebugProcess(e.getProject());
      if(process != null) {
        process.setWatchMethodReturnValuesEnabled(watch);
      }

    }
  }

  private static class AutoVarsSwitchAction extends ToggleAction {
    private volatile boolean myAutoModeEnabled;

    public AutoVarsSwitchAction() {
      super(DebuggerBundle.message("action.auto.variables.mode", new Object[0]), DebuggerBundle.message("action.auto.variables.mode.description", new Object[0]), (Icon)null);
      this.myAutoModeEnabled = DebuggerSettings.getInstance().AUTO_VARIABLES_MODE;
    }

    public boolean isSelected(AnActionEvent e) {
      return this.myAutoModeEnabled;
    }

    public void setSelected(AnActionEvent e, boolean enabled) {
      this.myAutoModeEnabled = enabled;
      DebuggerSettings.getInstance().AUTO_VARIABLES_MODE = enabled;
      XDebuggerUtilImpl.rebuildAllSessionsViews(e.getProject());
    }
  }
}
