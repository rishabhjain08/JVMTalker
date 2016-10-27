package api;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.intellij.core.CoreJavaFileManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.command.impl.DummyProject;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.impl.PlatformComponentManagerImpl;
import com.intellij.openapi.components.impl.ProjectPathMacroManager;
import com.intellij.openapi.components.impl.stores.IComponentStore;
import com.intellij.openapi.components.impl.stores.IProjectStore;
import com.intellij.openapi.components.impl.stores.StoreUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.impl.ModuleManagerImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.ex.ProjectEx.ProjectSaved;
import com.intellij.openapi.project.impl.ProjectLifecycleListener;
import com.intellij.openapi.project.impl.ProjectStoreClassProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.FrameTitleBuilder;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiElementFinderImpl;
import com.intellij.psi.impl.file.impl.JavaFileManagerImpl;
import com.intellij.util.ArrayUtil;
import com.intellij.util.TimedReference;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import com.intellij.util.pico.CachingConstructorInjectionComponentAdapter;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JFrame;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.PicoVisitor;
import org.picocontainer.alternatives.CachingPicoContainer;

class MyProjectImpl extends UserDataHolderBase implements Project {

    private static MyProjectImpl ourInstance;
    //private Map<String, List<Object>> componentImplementations;
    private CachingPicoContainer componentContainer;
    private CachingPicoContainer serviceContainer;
    private final MessageBus myMessageBus = MessageBusFactory.newMessageBus(this);
    private static PsiElementFinderImpl psiElementFinderImpl;
    MyCoreJavaFileManager coreJavaFileManager;

    @NotNull
    public static Project getInstance() {
        if (ourInstance == null) {
            ourInstance = new MyProjectImpl();
        }
        return ourInstance;
    }

    private MyProjectImpl() {
        componentContainer = new CachingPicoContainer();
        serviceContainer = new CachingPicoContainer();;//(CachingPicoContainer) componentContainer.makeChildContainer();
//        if (this.getComponent(PsiManager.class) == null) {
//            throw new Exception("PSIManger cannot be null in MyProjectImpl()");
//        }
        //componentImplementations = new ConcurrentHashMap<>();
    }

    public VirtualFile getProjectFile() {
        return null;
    }

    @NotNull
    public String getName() {
        return "";
    }

    @Nullable
    @NonNls
    public String getPresentableUrl() {
        return null;
    }

    @NotNull
    @NonNls
    public String getLocationHash() {
        return "dummy";
    }

    @Nullable
    public String getProjectFilePath() {
        return null;
    }

    public VirtualFile getWorkspaceFile() {
        return null;
    }

    @Nullable
    public VirtualFile getBaseDir() {
        return null;
    }

    @Nullable
    public String getBasePath() {
        return null;
    }

    public void save() {
    }

    public <T> void registerService(@NotNull String name, @NotNull T serviceImplementation) {
        serviceContainer.registerComponentInstance(name, serviceImplementation);
    }

    public <T> void registerService(@NotNull Class<T> serviceInterface, @NotNull T serviceImplementation) {
        serviceContainer.registerComponentInstance(serviceInterface, serviceImplementation);
    }

    public <T> void registerComponent(@NotNull Class<T> componentInterface, @NotNull T componentImplementation) {
        componentContainer.registerComponentInstance(componentInterface, componentImplementation);
    }

    public BaseComponent getComponent(@NotNull String name) {
        return (BaseComponent) componentContainer.getComponentInstance(name);
    }

    @Nullable
    public <T> T getComponent(@NotNull Class<T> interfaceClass) {
        return (T) componentContainer.getComponentInstanceOfType(interfaceClass);
    }

    public boolean hasComponent(@NotNull Class interfaceClass) {
        return getComponent(interfaceClass) != null;
    }

    @NotNull
    public <T> T[] getComponents(@NotNull Class<T> baseClass) {
        List<T> components = componentContainer.getComponentAdaptersOfType(baseClass);
        if (components == null) {
            return (T[]) Array.newInstance(baseClass, 0);
        }
        return (T[])componentContainer.getComponentAdaptersOfType(baseClass).toArray();
    }

    @NotNull
    public PicoContainer getPicoContainer() {
        return serviceContainer;
    }

    public <T> T getComponent(@NotNull Class<T> interfaceClass, T defaultImplementation) {
        return null;
    }

    public boolean isDisposed() {
        return false;
    }

    @NotNull
    public Condition getDisposed() {
        return (o) -> {
            return this.isDisposed();
        };
    }

    @NotNull
    public ComponentConfig[] getComponentConfigurations() {
        return new ComponentConfig[0];
    }

    @Nullable
    public Object getComponent(ComponentConfig componentConfig) {
        return null;
    }

    public boolean isOpen() {
        return false;
    }

    public boolean isInitialized() {
        return false;
    }

    public boolean isDefault() {
        return false;
    }

    @NotNull
    public MessageBus getMessageBus() {
        return myMessageBus;
    }

    public void dispose() {
    }

    @NotNull
    public <T> T[] getExtensions(@NotNull ExtensionPointName<T> extensionPointName) {
        if (extensionPointName.getName().equals("com.intellij.java.elementFinder")) {
            if (psiElementFinderImpl == null) {
                psiElementFinderImpl = new PsiElementFinderImpl(this, getCoreJavaFileManager());
            }
            return (T[]) (new PsiElementFinder[]{psiElementFinderImpl});
        }
        return (T[]) Array.newInstance(extensionPointName.getClass(), 0);
        //throw new UnsupportedOperationException("getExtensions()");
    }

    public void addClasspath(String path) {
        VirtualFileManagerImpl virtualFileManager = (VirtualFileManagerImpl) ApplicationManager.getApplication()
                .getComponent(VirtualFileManager.class);
        VirtualFile file = virtualFileManager.findFileByUrl("file://" + path);
        addClasspath(file);
    }

    public MyCoreJavaFileManager getCoreJavaFileManager() {
        if (coreJavaFileManager == null) {
            coreJavaFileManager = new MyCoreJavaFileManager(this.getComponent(PsiManager.class));
        }
        return coreJavaFileManager;
    }

    void addClasspath(VirtualFile myFile) {
        getCoreJavaFileManager().addToClasspath(myFile);
    }

    public ComponentConfig getConfig(Class componentImplementation) {
        throw new UnsupportedOperationException("Method getConfig not implemented in " + this.getClass());
    }
}

