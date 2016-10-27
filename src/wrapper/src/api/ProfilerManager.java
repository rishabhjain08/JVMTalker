package api;

import breakpoints.ProfilerLineBreakpointManager;
import com.intellij.core.CoreJavaFileManager;
import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DefaultSyntheticProvider;
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory;
import com.intellij.debugger.engine.SyntheticTypeComponentProvider;
import com.intellij.debugger.impl.DebuggerManagerImpl;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.filters.*;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.highlighter.FileTypeRegistrator;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.ide.util.ProjectPropertiesComponentImpl;
import com.intellij.lang.*;
import com.intellij.lang.impl.PsiBuilderFactoryImpl;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.mock.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.impl.DummyProject;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.extensions.*;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.impl.FileTypeManagerImpl;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.impl.ModuleManagerComponent;
import com.intellij.openapi.module.impl.ModuleManagerImpl;
import com.intellij.openapi.options.SchemesManagerFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.*;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.DirectoryIndexExcludePolicy;
import com.intellij.openapi.roots.impl.DirectoryIndexImpl;
import com.intellij.openapi.roots.impl.ProjectFileIndexImpl;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingManagerImpl;
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl;
import com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.psi.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.impl.file.PsiDirectoryFactoryImpl;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.impl.file.impl.FileManagerImpl;
import com.intellij.psi.impl.source.tree.JavaASTFactory;
import com.intellij.psi.search.*;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.testFramework.MockSchemesManagerFactory;
import com.intellij.util.CachedValuesManagerImpl;
import com.intellij.util.Function;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.IndexableSetContributor;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.impl.MessageBusImpl;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.XDebuggerManagerImpl;
import com.intellij.xdebugger.impl.XDebuggerUtilImpl;
import com.intellij.xdebugger.impl.evaluate.quick.common.ValueLookupManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.alternatives.EmptyPicoContainer;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * Created by rishajai on 10/14/16.
 */
class ProfilerManager {

    private static Project myProject;
    private static boolean isInitialized = false;
    private static ProfilerLineBreakpointManager profilerLineBreakpointManager;

    static {
        init();
    }

    static Project getProject() {
        return myProject;
    }

    static boolean isInitialized() {
        return isInitialized;
    }

    public synchronized static void init() {
        if (isInitialized) {
            return;
        }
        Disposable disposable = new Disposable() {
            @Override
            public void dispose() {

            }
        };
        Logger.setFactory(MyLoggerFactory.class);
        ApplicationManager.setApplication(new MockApplication(disposable), disposable);
        //
        myProject = MyProjectImpl.getInstance();
        Extensions.registerAreaClass(myProject.getClass().toString(), null);
        Extensions.instantiateArea(myProject.getClass().toString(), myProject, null);
        ExtensionsAreaImpl area = (ExtensionsAreaImpl) Extensions.getArea(myProject);
        ExtensionsAreaImpl rootArea = (ExtensionsAreaImpl) Extensions.getRootArea();
        area.registerExtensionPoint(new ExtensionPointImpl(PsiTreeChangePreprocessor.EP_NAME.getName(),
                        PsiTreeChangePreprocessor.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + PsiTreeChangePreprocessor.EP_NAME.getName()))));
        area.registerExtensionPoint(new ExtensionPointImpl(DirectoryIndexExcludePolicy.EP_NAME.getName(),
                DirectoryIndexExcludePolicy.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + DirectoryIndexExcludePolicy.EP_NAME.getName()))));



        ((MockComponentManager) ApplicationManager.getApplication()).addComponent(EditorFactory.class, new MockEditorFactory());
        ((MockComponentManager) ApplicationManager.getApplication()).registerService(EncodingManager.class, new
                EncodingManagerImpl(new MockEditorFactory()));
        ((MockComponentManager) ApplicationManager.getApplication()).registerService(PsiBuilderFactory.class,
                new PsiBuilderFactoryImpl());

                ((MockComponentManager) ApplicationManager.getApplication()).registerService(ProjectLocator.class, new
                ProjectLocator() {

                    @Nullable
                    @Override
                    public Project guessProjectForFile(VirtualFile virtualFile) {
                        return null;
                    }

                    @NotNull
                    @Override
                    public Collection<Project> getProjectsForFile(VirtualFile virtualFile) {
                        return null;
                    }
                });
        ((MockComponentManager) ApplicationManager.getApplication()).registerService(ProgressManager.class, new CoreProgressManager());

        registerVirtualFileManager();
        rootArea.registerExtensionPoint(new ExtensionPointImpl(XBreakpointType.EXTENSION_POINT_NAME.getName(),
                XBreakpointType.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + XBreakpointType.EXTENSION_POINT_NAME.getName()))));
        rootArea.registerExtensionPoint(new ExtensionPointImpl(AdditionalLibraryRootsProvider.EP_NAME.getName(),
                AdditionalLibraryRootsProvider.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + AdditionalLibraryRootsProvider.EP_NAME.getName()))));
        rootArea.registerExtensionPoint(new ExtensionPointImpl(SearchScopeEnlarger.EXTENSION.getName(),
                SearchScopeEnlarger.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + SearchScopeEnlarger.EXTENSION.getName()))));
        rootArea.registerExtensionPoint(new ExtensionPointImpl(FileTypeFactory.FILE_TYPE_FACTORY_EP.getName(),
                FileTypeFactory.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + FileTypeFactory.FILE_TYPE_FACTORY_EP.getName()))));
        rootArea.registerExtensionPoint(new ExtensionPointImpl(FileTypeRegistry.FileTypeDetector.EP_NAME.getName(),
                FileTypeRegistry.FileTypeDetector.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + FileTypeRegistry.FileTypeDetector.EP_NAME.getName()))));
        rootArea.registerExtensionPoint(new ExtensionPointImpl(FileTypeRegistrator.EP_NAME.getName(),
                FileTypeRegistrator.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + FileTypeRegistrator.EP_NAME.getName()))));
        ExtensionPointImpl extensionPoint;
        rootArea.registerExtensionPoint(extensionPoint = new ExtensionPointImpl<FileBasedIndexExtension>(FileBasedIndexExtension
                .EXTENSION_POINT_NAME
                .getName(),
                FileBasedIndexExtension.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + FileBasedIndexExtension.EXTENSION_POINT_NAME.getName()))));
        //extensionPoint.registerExtension(new FilenameIndex());

        rootArea.registerExtensionPoint(extensionPoint = new ExtensionPointImpl(IndexableSetContributor.EP_NAME.getName(),
                IndexableSetContributor.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + IndexableSetContributor.EP_NAME.getName()))));
        rootArea.registerExtensionPoint(extensionPoint = new ExtensionPointImpl<SyntheticTypeComponentProvider>(SyntheticTypeComponentProvider
                .EP_NAME.getName(),
                SyntheticTypeComponentProvider.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
                new Extensions.SimpleLogProvider(),
                new DefaultPluginDescriptor(("CUSTOM-" + SyntheticTypeComponentProvider.EP_NAME.getName()))));
        extensionPoint.registerExtension(new DefaultSyntheticProvider());


//        rootArea.registerExtensionPoint(extensionPoint = new ExtensionPointImpl<PsiElementFinder>(PsiElementFinder
//                .EP_NAME.getName(),
//                PsiElementFinder.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
//                new Extensions.SimpleLogProvider(),
//                new DefaultPluginDescriptor(("CUSTOM-" + PsiElementFinder.EP_NAME.getName()))));
//        extensionPoint.registerExtension(new PsiElementFinderImpl(myProject, new CoreJavaFileManager(myProject
//                .getComponent(PsiManager.class))));

        LanguageParserDefinitions.INSTANCE.addExplicitExtension(JavaLanguage.INSTANCE, new JavaParserDefinition());
        LanguageASTFactory.INSTANCE.addExplicitExtension(JavaLanguage.INSTANCE, new JavaASTFactory());
//        extensionPoint.registerExtension(new JavaParserDefinitionExtension());
//        ExtensionPointName<DebuggerSupport> DEBUGGER_SUPPORT_EXTENSION_POINT = ExtensionPointName.create("com" +
//                ".intellij" +
//                ".xdebugger.debuggerSupport");
//        rootArea.registerExtensionPoint(new ExtensionPointImpl(DEBUGGER_SUPPORT_EXTENSION_POINT.getName(),
//                DebuggerSupport.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
//                new Extensions.SimpleLogProvider(),
//                new DefaultPluginDescriptor(("CUSTOM-" + DEBUGGER_SUPPORT_EXTENSION_POINT.getName()))));
//        rootArea.registerExtensionPoint(new ExtensionPointImpl(JavaBreakpointHandlerFactory.EP_NAME.getName(),
//                JavaBreakpointHandlerFactory.class.getName(), ExtensionPoint.Kind.INTERFACE, area, myProject,
//                new Extensions.SimpleLogProvider(),
//                new DefaultPluginDescriptor(("CUSTOM-" + JavaBreakpointHandlerFactory.EP_NAME.getName()))));
        registerPsiManager();

        ((MyProjectImpl) myProject).registerComponent(XDebuggerManager.class, new XDebuggerManagerImpl(myProject, new
                StartupManagerImpl(myProject), myProject.getMessageBus()));
        ((MyProjectImpl) myProject).registerComponent(ProjectRootManager.class, new ProjectRootManagerImpl(myProject));
        ((MyProjectImpl) myProject).registerComponent(ModuleManager.class, new ModuleManagerComponent(myProject,
                new CoreProgressManager(), myProject.getMessageBus()));


        ((MyProjectImpl) myProject).registerService(PsiDirectoryFactory.class.getName(), new PsiDirectoryFactoryImpl(
                (PsiManagerImpl) (myProject.getComponent(PsiManager.class))));
//        ((MyProjectImpl) myProject).registerService(PsiDirectoryFactory.class, new PsiDirectoryFactoryImpl(
//                (PsiManagerImpl) (myProject.getComponent(PsiManager.class))));
                ((MyProjectImpl) myProject).registerService(DumbService.class.getName(), new DumbServiceImpl(myProject));
        ((MyProjectImpl) myProject).registerService(CachedValuesManager.class.getName(), new CachedValuesManagerImpl
                (myProject, new PsiCachedValuesFactory(((MyProjectImpl) myProject).getComponent(PsiManager.class))));
        ((MyProjectImpl) myProject).registerService(JavaPsiImplementationHelper.class.getName(), new
                JavaPsiImplementationHelperImpl(myProject));
        ((MyProjectImpl) myProject).registerService(JavaPsiFacade.class.getName(),
                new JavaPsiFacadeImpl(myProject, myProject.getComponent(PsiManager.class), null, myProject.getMessageBus()));


        //        ((MyProjectImpl) myProject).registerService(ValueLookupManager.class.getName(), new ValueLookupManager(myProject));
        ((MyProjectImpl) myProject).registerService(ProjectScopeBuilder.class.getName(), new ProjectScopeBuilderImpl(myProject));
//        ((MyProjectImpl) myProject).registerService(ExecutionManager.class.getName(), new ExecutionManagerImpl(myProject));
//        FileTypeRegistry fileTypeRegistry = new MyFileTypeRegistryG();
//        Getter<FileTypeRegistry>() {
//            @Override
//            public FileTypeRegistry get() {
//                return fileTypeRegistry;
//            }
//        };
        FileTypeManagerImpl fileTypeManager = new FileTypeManagerImpl(myProject.getMessageBus(),
                new MockSchemesManagerFactory(), new ProjectPropertiesComponentImpl());
        ((MyProjectImpl) myProject).registerService(ProjectFileIndex.class.getName(),
                new ProjectFileIndexImpl(myProject, new DirectoryIndexImpl(myProject), fileTypeManager));
        FileTypeRegistry.ourInstanceGetter = new Getter<FileTypeRegistry>() {
            @Override
            public FileTypeRegistry get() {
                return new FileTypeRegistry() {
                    @Override
                    public boolean isFileIgnored(@NotNull VirtualFile virtualFile) {
                        return false;
                    }

                    @Override
                    public FileType[] getRegisteredFileTypes() {
                        return new FileType[]{JavaFileType.INSTANCE};
                    }

                    @NotNull
                    @Override
                    public FileType getFileTypeByFile(@NotNull VirtualFile virtualFile) {
                        return JavaFileType.INSTANCE;
                    }

                    @NotNull
                    @Override
                    public FileType getFileTypeByFileName(@NotNull @NonNls String s) {
                        return JavaFileType.INSTANCE;
                    }

                    @NotNull
                    @Override
                    public FileType getFileTypeByExtension(@NonNls @NotNull String s) {
                        return JavaFileType.INSTANCE;
                    }

                    @NotNull
                    @Override
                    public FileType detectFileTypeFromContent(@NotNull VirtualFile virtualFile) {
                        return JavaFileType.INSTANCE;
                    }

                    @Nullable
                    @Override
                    public FileType findFileTypeByName(@NotNull String s) {
                        return JavaFileType.INSTANCE;
                    }
                };
            }
        };

        ((MockComponentManager) ApplicationManager.getApplication()).registerService(TextConsoleBuilderFactory.class,
                new TextConsoleBuilderFactory(){

                    private TextConsoleBuilder builder = new TextConsoleBuilder() {

                        @Override
                        public ConsoleView getConsole() {
                            return new ConsoleView() {
                                @Override
                                public void print(@NotNull String s, @NotNull ConsoleViewContentType consoleViewContentType) {

                                }

                                @Override
                                public void clear() {

                                }

                                @Override
                                public void scrollTo(int i) {

                                }

                                @Override
                                public void attachToProcess(ProcessHandler processHandler) {

                                }

                                @Override
                                public void setOutputPaused(boolean b) {

                                }

                                @Override
                                public boolean isOutputPaused() {
                                    return false;
                                }

                                @Override
                                public boolean hasDeferredOutput() {
                                    return false;
                                }

                                @Override
                                public void performWhenNoDeferredOutput(Runnable runnable) {

                                }

                                @Override
                                public void setHelpId(String s) {

                                }

                                @Override
                                public void addMessageFilter(Filter filter) {

                                }

                                @Override
                                public void printHyperlink(String s, HyperlinkInfo hyperlinkInfo) {

                                }

                                @Override
                                public int getContentSize() {
                                    return 0;
                                }

                                @Override
                                public boolean canPause() {
                                    return false;
                                }

                                @NotNull
                                @Override
                                public AnAction[] createConsoleActions() {
                                    return new AnAction[0];
                                }

                                @Override
                                public void allowHeavyFilters() {

                                }

                                @Override
                                public JComponent getComponent() {
                                    return null;
                                }

                                @Override
                                public JComponent getPreferredFocusableComponent() {
                                    return null;
                                }

                                @Override
                                public void dispose() {

                                }
                            };
                        }

                        @Override
                        public void addFilter(Filter filter) {

                        }

                        @Override
                        public void setViewer(boolean b) {

                        }
                    };
                    @Override
                    public TextConsoleBuilder createBuilder(@NotNull Project project) {
                        return builder;
                    }

                    @Override
                    public TextConsoleBuilder createBuilder(@NotNull Project project, @NotNull GlobalSearchScope globalSearchScope) {
                        return builder;
                    }
                });
        registerFileDocumentManager();
        registerDebuggerManager();
//        FileBasedIndexImpl fileBasedIndex;
        MockComponentManager applicationComponentManager = (MockComponentManager) ApplicationManager.getApplication();
//        ((MockComponentManager) ApplicationManager
//                .getApplication()).addComponent(FileBasedIndex.class,
//                fileBasedIndex =new FileBasedIndexImpl(applicationComponentManager.getComponent(VirtualFileManager.class),
//                        applicationComponentManager.getComponent(FileDocumentManager.class), fileTypeManager,
//                        myProject.getMessageBus()));
//        fileBasedIndex.initComponent();
        isInitialized = true;
//        FSRecords.DbConnection.flushSome();//.invalidateCaches();//.dispose();
        profilerLineBreakpointManager = ProfilerLineBreakpointManager.getProfilerLineBreakpointManager(myProject);
        System.out.println("Done initializing...");

    }

    private static void registerFileDocumentManager() {
        MockComponentManager mockComponentManager = (MockComponentManager) ApplicationManager.getApplication();
        MyProjectImpl projectComponentManager = (MyProjectImpl) myProject;
        Function textToDoc = new Function<CharSequence, Document>(){

            @Override
            public Document fun(CharSequence charSequence) {
                return new DocumentImpl(charSequence);
            }
        };
        MockFileDocumentManagerImpl documentManager = new MockFileDocumentManagerImpl(textToDoc, new Key<Document>("MY_CACHED_DOCUMENT_KEY"));
        projectComponentManager.registerComponent(FileDocumentManager.class, documentManager);
        mockComponentManager.registerService(FileDocumentManager.class, documentManager);
    }

    private static void registerDebuggerManager() {
        MockComponentManager mockComponentManager = (MockComponentManager) ApplicationManager.getApplication();
        MyProjectImpl projectComponentManager = (MyProjectImpl) myProject;
        Function textToDoc = new Function<CharSequence, Document>(){

            @Override
            public Document fun(CharSequence charSequence) {
                return new DocumentImpl(charSequence);
            }
        };
        DebuggerManagerImpl debuggerManager = new DebuggerManagerImpl(myProject);
        projectComponentManager.registerComponent(DebuggerManager.class, debuggerManager);
        mockComponentManager.registerService(DebuggerManager.class, debuggerManager);
        mockComponentManager.registerService(XDebuggerUtil.class, new XDebuggerUtilImpl());
    }

    private static void registerVirtualFileManager() {
        MockComponentManager mockComponentManager = (MockComponentManager) ApplicationManager.getApplication();
        MyProjectImpl projectComponentManager = (MyProjectImpl) myProject;
//        LocalFileSystemImpl localFileSystem = new LocalFileSystemImpl(ApplicationManager.getApplication(),
//                new PersistentFSImpl(mockComponentManager.getMessageBus()));
        MyLocalFileSystem localFileSystem = new MyLocalFileSystem();
        VirtualFileManagerImpl virtualFileManager = new VirtualFileManagerImpl(new VirtualFileSystem[]{localFileSystem}, mockComponentManager.getMessageBus());
//        mockComponentManager.registerService(ManagingFS.class, new PersistentFSImpl(mockComponentManager.getMessageBus()));
        System.setProperty("caches_dir", "/Users/rishajai/Desktop/caches_dir/all_caches/caches_dir");
        System.setProperty("idea.home.path", "/Users/rishajai/Desktop/caches_dir/all_caches/idea_home");
        PersistentFSImpl persistentFS = new MyManagingFS(myProject.getMessageBus());
//        new PersistentFSImpl(mockComponentManager.getMessageBus());
        mockComponentManager.registerService(ManagingFS.class, persistentFS);
        persistentFS.initComponent();
        persistentFS.cleanPersistedContents();
        projectComponentManager.registerComponent(VirtualFileManager.class, virtualFileManager);
        mockComponentManager.registerService(VirtualFileManager.class, virtualFileManager);
    }

    private static void registerPsiManager() {
        Application application = ApplicationManager.getApplication();
        MockComponentManager mockComponentManager = (MockComponentManager) ApplicationManager.getApplication();
        MyProjectImpl projectComponentManager = (MyProjectImpl) myProject;
        PsiModificationTrackerImpl psiModificationTracker = new PsiModificationTrackerImpl(myProject);
        PsiManagerImpl psiManager = new PsiManagerImpl(myProject, mockComponentManager.getComponent(FileDocumentManager
                .class), null, new MockFileIndexFacade(myProject), application.getMessageBus(), new
                PsiModificationTrackerImpl(myProject));
        ((FileManagerImpl) psiManager.getFileManager()).markInitialized();
        projectComponentManager.registerComponent(PsiManager.class, psiManager);
        mockComponentManager.registerService(PsiManager.class, psiManager);

        projectComponentManager.registerComponent(PsiDocumentManager.class, new PsiDocumentManagerImpl(myProject,
                psiManager, new MockEditorFactory(), myProject.getMessageBus(), new DocumentCommitProcessor(){
            @Override
            public void commitSynchronously(@NotNull Document document, @NotNull Project project, @NotNull PsiFile psiFile) {

            }

            @Override
            public void commitAsynchronously(@NotNull Project project, @NotNull Document document, @NonNls @NotNull Object o, @NotNull ModalityState modalityState) {

            }
        }));

    }

    public static ProfilerLineBreakpointManager getProfilerLineBreakpointManager() {
        return profilerLineBreakpointManager;
    }
}
