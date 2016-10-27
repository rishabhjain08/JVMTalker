//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intellij.debugger.ui.impl.watch;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.ContextUtil;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.settings.ThreadsViewSettings;
import com.intellij.debugger.ui.impl.watch.MethodsTracker;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.MethodsTracker.MethodOccurrence;
import com.intellij.debugger.ui.tree.StackFrameDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.icons.AllIcons.Debugger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.FileColorManager;
import com.intellij.util.StringBuilderSpinAllocator;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.frame.XValueMarkers;
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.InternalException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import java.awt.Color;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StackFrameDescriptorImpl extends NodeDescriptorImpl implements StackFrameDescriptor {
  private final StackFrameProxyImpl myFrame;
  private int myUiIndex;
  private String myName = null;
  private Location myLocation;
  private MethodOccurrence myMethodOccurrence;
  private boolean myIsSynthetic;
  private boolean myIsInLibraryContent;
  private ObjectReference myThisObject;
  private Color myBackgroundColor;
  private SourcePosition mySourcePosition;
  private Icon myIcon;

  public StackFrameDescriptorImpl(@NotNull StackFrameProxyImpl frame, @NotNull MethodsTracker tracker) {
    this.myIcon = Debugger.StackFrame;
    this.myFrame = frame;

    try {
      this.myUiIndex = frame.getFrameIndex();
      this.myLocation = frame.location();

      try {
        this.myThisObject = frame.thisObject();
      } catch (EvaluateException var4) {
        if(!(var4.getCause() instanceof InternalException)) {
          throw var4;
        }

        LOG.info(var4);
      }

      this.myMethodOccurrence = tracker.getMethodOccurrence(this.myUiIndex, this.myLocation.method());
      this.myIsSynthetic = DebuggerUtils.isSynthetic(this.myMethodOccurrence.getMethod());
      ApplicationManager.getApplication().runReadAction(() -> {
        this.mySourcePosition = ContextUtil.getSourcePosition(this);
        PsiFile file = this.mySourcePosition != null?this.mySourcePosition.getFile():null;
        if(file == null) {
          this.myIsInLibraryContent = true;
        } else {
          this.myBackgroundColor = FileColorManager.getInstance(file.getProject()).getFileColor(file);
          ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(this.getDebugProcess().getProject()).getFileIndex();
          VirtualFile vFile = file.getVirtualFile();
          this.myIsInLibraryContent = vFile != null && (projectFileIndex.isInLibraryClasses(vFile) || projectFileIndex.isInLibrarySource(vFile));
        }

      });
    } catch (EvaluateException | InternalException var5) {
      LOG.info(var5);
      this.myLocation = null;
      this.myMethodOccurrence = tracker.getMethodOccurrence(0, (Method)null);
      this.myIsSynthetic = false;
      this.myIsInLibraryContent = false;
    }

  }

  public int getUiIndex() {
    return this.myUiIndex;
  }

  @NotNull
  public StackFrameProxyImpl getFrameProxy() {
    StackFrameProxyImpl var10000 = this.myFrame;
    if(this.myFrame == null) {
      throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/intellij/debugger/ui/impl/watch/StackFrameDescriptorImpl", "getFrameProxy"}));
    } else {
      return var10000;
    }
  }

  @NotNull
  public DebugProcess getDebugProcess() {
    return this.myFrame.getVirtualMachine().getDebugProcess();
  }

  public Color getBackgroundColor() {
    return this.myBackgroundColor;
  }

  @Nullable
  public Method getMethod() {
    return this.myMethodOccurrence.getMethod();
  }

  public int getOccurrenceIndex() {
    return this.myMethodOccurrence.getIndex();
  }

  public boolean isRecursiveCall() {
    return this.myMethodOccurrence.isRecursive();
  }

  @Nullable
  public ValueMarkup getValueMarkup() {
    if(this.myThisObject != null) {
      DebugProcess process = this.myFrame.getVirtualMachine().getDebugProcess();
      if(process instanceof DebugProcessImpl) {
        XDebugSession session = ((DebugProcessImpl)process).getSession().getXDebugSession();
        if(session instanceof XDebugSessionImpl) {
          XValueMarkers markers = ((XDebugSessionImpl)session).getValueMarkers();
          if(markers != null) {
            return (ValueMarkup)markers.getAllMarkers().get(this.myThisObject);
          }
        }
      }
    }

    return null;
  }

  public String getName() {
    return this.myName;
  }

  protected String calcRepresentation(EvaluationContextImpl context, DescriptorLabelListener descriptorLabelListener) throws EvaluateException {
    DebuggerManagerThreadImpl.assertIsManagerThread();
    if(this.myLocation == null) {
      return "";
    } else {
      ThreadsViewSettings settings = ThreadsViewSettings.getInstance();
      StringBuilder label = StringBuilderSpinAllocator.alloc();

      String sourceName;
      try {
        Method method = this.myMethodOccurrence.getMethod();
        if(method != null) {
          this.myName = method.name();
          label.append(settings.SHOW_ARGUMENTS_TYPES?DebuggerUtilsEx.methodNameWithArguments(method):this.myName);
        }

        if(settings.SHOW_LINE_NUMBER) {
          try {
            sourceName = Integer.toString(this.myLocation.lineNumber());
          } catch (InternalError var17) {
            sourceName = var17.toString();
          }

          if(sourceName != null) {
            label.append(':');
            label.append(sourceName);
          }
        }

        if(settings.SHOW_CLASS_NAME) {
          try {
            ReferenceType e = this.myLocation.declaringType();
            sourceName = e != null?e.name():null;
          } catch (InternalError var16) {
            sourceName = var16.toString();
          }

          if(sourceName != null) {
            label.append(", ");
            int e1 = sourceName.lastIndexOf(46);
            if(e1 < 0) {
              label.append(sourceName);
            } else {
              label.append(sourceName.substring(e1 + 1));
              if(settings.SHOW_PACKAGE_NAME) {
                label.append(" {");
                label.append(sourceName.substring(0, e1));
                label.append("}");
              }
            }
          }
        }

        if(settings.SHOW_SOURCE_NAME) {
          try {
            try {
              sourceName = this.myLocation.sourceName();
            } catch (InternalError var14) {
              sourceName = var14.toString();
            }

            label.append(", ");
            label.append(sourceName);
          } catch (AbsentInformationException var15) {
            ;
          }
        }

        sourceName = label.toString();
      } finally {
        StringBuilderSpinAllocator.dispose(label);
      }

      return sourceName;
    }
  }

  public final boolean stackFramesEqual(StackFrameDescriptorImpl d) {
    return this.getFrameProxy().equals(d.getFrameProxy());
  }

  public boolean isExpandable() {
    return true;
  }

  public final void setContext(EvaluationContextImpl context) {
    this.myIcon = this.calcIcon();
  }

  public boolean isSynthetic() {
    return this.myIsSynthetic;
  }

  public boolean isInLibraryContent() {
    return this.myIsInLibraryContent;
  }

  @Nullable
  public Location getLocation() {
    return this.myLocation;
  }

  public SourcePosition getSourcePosition() {
    return this.mySourcePosition;
  }

  private Icon calcIcon() {
    try {
      if(this.myFrame.isObsolete()) {
        return Debugger.Db_obsolete;
      }
    } catch (EvaluateException var2) {
      ;
    }

    return EmptyIcon.create(6);
  }

  public Icon getIcon() {
    return this.myIcon;
  }

  public ObjectReference getThisObject() {
    return this.myThisObject;
  }
}
