//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package api;

import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.util.io.FileSystemUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileOperationsHandler;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.SafeWriteRequestor;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VfsBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.ex.VirtualFileManagerEx;
import com.intellij.openapi.vfs.impl.local.LocalFileSystemBase;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.VfsImplUtil;
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathUtilRt;
import com.intellij.util.Processor;
import com.intellij.util.ThrowableConsumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.SafeFileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class MyLocalFileSystem extends LocalFileSystemBase {
    protected static final Logger LOG = Logger.getInstance(MyLocalFileSystem.class);
    private static final FileAttributes FAKE_ROOT_ATTRIBUTES = new FileAttributes(true, false, false, false, 0L, 0L, false);
    private final List<LocalFileOperationsHandler> myHandlers = new ArrayList();

    public MyLocalFileSystem() {
    }

    @Nullable
    public VirtualFile findFileByPath(@NotNull String path) {
        return VfsImplUtil.findFileByPath(this, path);
    }

    public VirtualFile findFileByPathIfCached(@NotNull String path) {
        return VfsImplUtil.findFileByPathIfCached(this, path);
    }

    @Nullable
    public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
        return VfsImplUtil.refreshAndFindFileByPath(this, path);
    }

    public VirtualFile findFileByIoFile(@NotNull File file) {
        String path = FileUtil.toSystemIndependentName(file.getAbsolutePath());
        return this.findFileByPath(path);
    }

    @NotNull
    protected static File convertToIOFile(@NotNull VirtualFile file) {
        String path = file.getPath();
        if(StringUtil.endsWithChar(path, ':') && path.length() == 2 && (SystemInfo.isWindows || SystemInfo.isOS2)) {
            path = path + "/";
        }

        return new File(path);
    }

    @NotNull
    private static File convertToIOFileAndCheck(@NotNull VirtualFile file) throws FileNotFoundException {
        File ioFile = convertToIOFile(file);
        FileAttributes attributes = FileSystemUtil.getAttributes(ioFile);
        if(attributes != null && !attributes.isFile()) {
            LOG.warn("not a file: " + ioFile + ", " + attributes);
            throw new FileNotFoundException("Not a file: " + ioFile);
        } else {
            return ioFile;
        }
    }

    public boolean exists(@NotNull VirtualFile file) {
        return this.getAttributes(file) != null;
    }

    public long getLength(@NotNull VirtualFile file) {
        FileAttributes attributes = this.getAttributes(file);
        return attributes != null?attributes.length:0L;
    }

    public long getTimeStamp(@NotNull VirtualFile file) {
        FileAttributes attributes = this.getAttributes(file);
        return attributes != null?attributes.lastModified:0L;
    }

    public boolean isDirectory(@NotNull VirtualFile file) {
        FileAttributes attributes = this.getAttributes(file);
        return attributes != null && attributes.isDirectory();
    }

    public boolean isWritable(@NotNull VirtualFile file) {
        FileAttributes attributes = this.getAttributes(file);
        return attributes != null && attributes.isWritable();
    }

    public boolean isSymLink(@NotNull VirtualFile file) {
        FileAttributes attributes = this.getAttributes(file);
        return attributes != null && attributes.isSymLink();
    }

    public String resolveSymLink(@NotNull VirtualFile file) {
        return FileSystemUtil.resolveSymLink(file.getPath());
    }

    @NotNull
    public String[] list(@NotNull VirtualFile file) {
        String[] var10000;
        if(file.getParent() == null) {
            File[] names = File.listRoots();
            String[] names1;
            if(names.length == 1 && names[0].getName().isEmpty()) {
                names1 = names[0].list();
                if(names1 != null) {
                    return names1;
                }

                LOG.warn("Root \'" + names[0] + "\' has no children - is it readable?");
                var10000 = ArrayUtil.EMPTY_STRING_ARRAY;
                if(ArrayUtil.EMPTY_STRING_ARRAY == null) {
                    throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/intellij/openapi/vfs/impl/local/LocalFileSystemBase", "list"}));
                }

                return var10000;
            }

            if(file.getName().isEmpty()) {
                names1 = new String[names.length];

                for(int i = 0; i < names1.length; ++i) {
                    String name = names[i].getPath();
                    name = StringUtil.trimEnd(name, File.separator);
                    names1[i] = name;
                }

                return names1;
            }
        }

        String[] var6 = convertToIOFile(file).list();
        var10000 = var6 == null?ArrayUtil.EMPTY_STRING_ARRAY:var6;
        if((var6 == null?ArrayUtil.EMPTY_STRING_ARRAY:var6) == null) {
            throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/intellij/openapi/vfs/impl/local/LocalFileSystemBase", "list"}));
        } else {
            return var10000;
        }
    }

    @NotNull
    public String getProtocol() {
        return "file";
    }

    public boolean isReadOnly() {
        return false;
    }

    @Nullable
    protected String normalize(@NotNull String path) {
        if(path.isEmpty()) {
            try {
                path = (new File("")).getCanonicalPath();
            } catch (IOException var4) {
                return path;
            }
        } else if(SystemInfo.isWindows) {
            if(path.charAt(0) == 47 && !path.startsWith("//")) {
                path = path.substring(1);
            }

            try {
                path = FileUtil.resolveShortWindowsName(path);
            } catch (IOException var3) {
                return null;
            }
        }

        File file = new File(path);
        if(!isAbsoluteFileOrDriveLetter(file)) {
            path = file.getAbsolutePath();
        }

        return FileUtil.normalize(path);
    }

    private static boolean isAbsoluteFileOrDriveLetter(@NotNull File file) {
        String path = file.getPath();
        return SystemInfo.isWindows && path.length() == 2 && path.charAt(1) == 58?true:file.isAbsolute();
    }

    public VirtualFile refreshAndFindFileByIoFile(@NotNull File file) {
        String path = FileUtil.toSystemIndependentName(file.getAbsolutePath());
        return this.refreshAndFindFileByPath(path);
    }

    public void refreshIoFiles(@NotNull Iterable<File> files) {
        this.refreshIoFiles(files, false, false, (Runnable)null);
    }

    public void refreshIoFiles(@NotNull Iterable<File> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {
        VirtualFileManagerEx manager = (VirtualFileManagerEx)VirtualFileManager.getInstance();
        Application app = ApplicationManager.getApplication();
        boolean fireCommonRefreshSession = app.isDispatchThread() || app.isWriteAccessAllowed();
        if(fireCommonRefreshSession) {
            manager.fireBeforeRefreshStart(false);
        }

        try {
            ArrayList filesToRefresh = new ArrayList();
            Iterator var9 = files.iterator();

            while(var9.hasNext()) {
                File file = (File)var9.next();
                VirtualFile virtualFile = this.refreshAndFindFileByIoFile(file);
                if(virtualFile != null) {
                    filesToRefresh.add(virtualFile);
                }
            }

            RefreshQueue.getInstance().refresh(async, recursive, onFinish, filesToRefresh);
        } finally {
            if(fireCommonRefreshSession) {
                manager.fireAfterRefreshFinish(false);
            }

        }
    }

    public void refreshFiles(@NotNull Iterable<VirtualFile> files) {
        this.refreshFiles(files, false, false, (Runnable)null);
    }

    public void refreshFiles(@NotNull Iterable<VirtualFile> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {
        RefreshQueue.getInstance().refresh(async, recursive, onFinish, ContainerUtil.toCollection(files));
    }

    @NotNull
    @Override
    public Set<WatchRequest> replaceWatchedRoots(@NotNull Collection<WatchRequest> collection, @Nullable Collection<String> collection1, @Nullable Collection<String> collection2) {
        return null;
    }

    public void registerAuxiliaryFileOperationsHandler(@NotNull LocalFileOperationsHandler handler) {
        if(this.myHandlers.contains(handler)) {
            LOG.error("Handler " + handler + " already registered.");
        }

        this.myHandlers.add(handler);
    }

    public void unregisterAuxiliaryFileOperationsHandler(@NotNull LocalFileOperationsHandler handler) {
        if(!this.myHandlers.remove(handler)) {
            LOG.error("Handler" + handler + " haven\'t been registered or already unregistered.");
        }

    }

    public boolean processCachedFilesInSubtree(@NotNull VirtualFile file, @NotNull Processor<VirtualFile> processor) {
        return file.getFileSystem() != this || processFile((NewVirtualFile)file, processor);
    }

    private static boolean processFile(@NotNull NewVirtualFile file, @NotNull Processor<VirtualFile> processor) {
        if(!processor.process(file)) {
            return false;
        } else {
            if(file.isDirectory()) {
                Iterator var2 = file.getCachedChildren().iterator();

                while(var2.hasNext()) {
                    VirtualFile child = (VirtualFile)var2.next();
                    if(!processFile((NewVirtualFile)child, processor)) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    private boolean auxDelete(@NotNull VirtualFile file) throws IOException {
        Iterator var2 = this.myHandlers.iterator();

        LocalFileOperationsHandler handler;
        do {
            if(!var2.hasNext()) {
                return false;
            }

            handler = (LocalFileOperationsHandler)var2.next();
        } while(!handler.delete(file));

        return true;
    }

    private boolean auxMove(@NotNull VirtualFile file, @NotNull VirtualFile toDir) throws IOException {
        Iterator var3 = this.myHandlers.iterator();

        LocalFileOperationsHandler handler;
        do {
            if(!var3.hasNext()) {
                return false;
            }

            handler = (LocalFileOperationsHandler)var3.next();
        } while(!handler.move(file, toDir));

        return true;
    }

    private boolean auxCopy(@NotNull VirtualFile file, @NotNull VirtualFile toDir, @NotNull String copyName) throws IOException {
        Iterator var4 = this.myHandlers.iterator();

        File copy;
        do {
            if(!var4.hasNext()) {
                return false;
            }

            LocalFileOperationsHandler handler = (LocalFileOperationsHandler)var4.next();
            copy = handler.copy(file, toDir, copyName);
        } while(copy == null);

        return true;
    }

    private boolean auxRename(@NotNull VirtualFile file, @NotNull String newName) throws IOException {
        Iterator var3 = this.myHandlers.iterator();

        LocalFileOperationsHandler handler;
        do {
            if(!var3.hasNext()) {
                return false;
            }

            handler = (LocalFileOperationsHandler)var3.next();
        } while(!handler.rename(file, newName));

        return true;
    }

    private boolean auxCreateFile(@NotNull VirtualFile dir, @NotNull String name) throws IOException {
        Iterator var3 = this.myHandlers.iterator();

        LocalFileOperationsHandler handler;
        do {
            if(!var3.hasNext()) {
                return false;
            }

            handler = (LocalFileOperationsHandler)var3.next();
        } while(!handler.createFile(dir, name));

        return true;
    }

    private boolean auxCreateDirectory(@NotNull VirtualFile dir, @NotNull String name) throws IOException {
        Iterator var3 = this.myHandlers.iterator();

        LocalFileOperationsHandler handler;
        do {
            if(!var3.hasNext()) {
                return false;
            }

            handler = (LocalFileOperationsHandler)var3.next();
        } while(!handler.createDirectory(dir, name));

        return true;
    }

    private void auxNotifyCompleted(@NotNull ThrowableConsumer<LocalFileOperationsHandler, IOException> consumer) {
        Iterator var2 = this.myHandlers.iterator();

        while(var2.hasNext()) {
            LocalFileOperationsHandler handler = (LocalFileOperationsHandler)var2.next();
            handler.afterDone(consumer);
        }

    }

    @NotNull
    public VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile parent, @NotNull String dir) throws IOException {
        if(!this.isValidName(dir)) {
            throw new IOException(VfsBundle.message("directory.invalid.name.error", new Object[]{dir}));
        } else if(parent.exists() && parent.isDirectory()) {
            if(parent.findChild(dir) != null) {
                throw new IOException(VfsBundle.message("vfs.target.already.exists.error", new Object[]{parent.getPath() + "/" + dir}));
            } else {
                File ioParent = convertToIOFile(parent);
                if(!ioParent.isDirectory()) {
                    throw new IOException(VfsBundle.message("target.not.directory.error", new Object[]{ioParent.getPath()}));
                } else {
                    if(!this.auxCreateDirectory(parent, dir)) {
                        File ioDir = new File(ioParent, dir);
                        if(!ioDir.mkdirs() && !ioDir.isDirectory()) {
                            throw new IOException(VfsBundle.message("new.directory.failed.error", new Object[]{ioDir.getPath()}));
                        }
                    }

                    this.auxNotifyCompleted((handler) -> {
                        handler.createDirectory(parent, dir);
                    });
                    return new FakeVirtualFile(parent, dir);
                }
            }
        } else {
            throw new IOException(VfsBundle.message("vfs.target.not.directory.error", new Object[]{parent.getPath()}));
        }
    }

    @NotNull
    public VirtualFile createChildFile(Object requestor, @NotNull VirtualFile parent, @NotNull String file) throws IOException {
        if(!this.isValidName(file)) {
            throw new IOException(VfsBundle.message("file.invalid.name.error", new Object[]{file}));
        } else if(parent.exists() && parent.isDirectory()) {
            if(parent.findChild(file) != null) {
                throw new IOException(VfsBundle.message("vfs.target.already.exists.error", new Object[]{parent.getPath() + "/" + file}));
            } else {
                File ioParent = convertToIOFile(parent);
                if(!ioParent.isDirectory()) {
                    throw new IOException(VfsBundle.message("target.not.directory.error", new Object[]{ioParent.getPath()}));
                } else {
                    if(!this.auxCreateFile(parent, file)) {
                        File ioFile = new File(ioParent, file);
                        if(!FileUtil.createIfDoesntExist(ioFile)) {
                            throw new IOException(VfsBundle.message("new.file.failed.error", new Object[]{ioFile.getPath()}));
                        }
                    }

                    this.auxNotifyCompleted((handler) -> {
                        handler.createFile(parent, file);
                    });
                    return new FakeVirtualFile(parent, file);
                }
            }
        } else {
            throw new IOException(VfsBundle.message("vfs.target.not.directory.error", new Object[]{parent.getPath()}));
        }
    }

    public void deleteFile(Object requestor, @NotNull VirtualFile file) throws IOException {
        if(file.getParent() == null) {
            throw new IOException(VfsBundle.message("cannot.delete.root.directory", new Object[]{file.getPath()}));
        } else {
            if(!this.auxDelete(file)) {
                File ioFile = convertToIOFile(file);
                if(!FileUtil.delete(ioFile)) {
                    throw new IOException(VfsBundle.message("delete.failed.error", new Object[]{ioFile.getPath()}));
                }
            }

            this.auxNotifyCompleted((handler) -> {
                handler.delete(file);
            });
        }
    }

    public boolean isCaseSensitive() {
        return SystemInfo.isFileSystemCaseSensitive;
    }

    public boolean isValidName(@NotNull String name) {
        return PathUtilRt.isValidFileName(name, false);
    }

    @NotNull
    public InputStream getInputStream(@NotNull VirtualFile file) throws IOException {
        return new BufferedInputStream(new FileInputStream(convertToIOFileAndCheck(file)));
    }

    @NotNull
    public byte[] contentsToByteArray(@NotNull VirtualFile file) throws IOException {
        FileInputStream stream = new FileInputStream(convertToIOFileAndCheck(file));

        byte[] var6;
        try {
            long l = file.getLength();
            if(l > 2147483647L) {
                throw new IOException("File is too large: " + l + ", " + file);
            }

            int length = (int)l;
            if(length < 0) {
                throw new IOException("Invalid file length: " + length + ", " + file);
            }

            var6 = loadBytes((InputStream)(length <= 8192?stream:new BufferedInputStream(stream)), length);
        } finally {
            stream.close();
        }

        return var6;
    }

    @NotNull
    private static byte[] loadBytes(@NotNull InputStream stream, int length) throws IOException {
        byte[] bytes = new byte[length];

        int count;
        int n;
        for(count = 0; count < length; count += n) {
            n = stream.read(bytes, count, length - count);
            if(n <= 0) {
                break;
            }
        }

        return count < length?Arrays.copyOf(bytes, count):bytes;
    }

    @NotNull
    public OutputStream getOutputStream(@NotNull VirtualFile file, Object requestor, long modStamp, final long timeStamp) throws IOException {
        final File ioFile = convertToIOFileAndCheck(file);
        final Object stream = shallUseSafeStream(requestor, file)?new SafeFileOutputStream(ioFile, SystemInfo.isUnix):new FileOutputStream(ioFile);
        return new BufferedOutputStream((OutputStream)stream) {
            public void close() throws IOException {
                super.close();
                if(timeStamp > 0L && ioFile.exists() && !ioFile.setLastModified(timeStamp)) {
                    LocalFileSystemBase.LOG.warn("Failed: " + ioFile.getPath() + ", new:" + timeStamp + ", old:" + ioFile.lastModified());
                }

            }
        };
    }

    private static boolean shallUseSafeStream(Object requestor, @NotNull VirtualFile file) {
        return requestor instanceof SafeWriteRequestor && GeneralSettings.getInstance().isUseSafeWrite() && !file.is(VFileProperty.SYMLINK);
    }

    public void moveFile(Object requestor, @NotNull VirtualFile file, @NotNull VirtualFile newParent) throws IOException {
        String name = file.getName();
        if(!file.exists()) {
            throw new IOException(VfsBundle.message("vfs.file.not.exist.error", new Object[]{file.getPath()}));
        } else if(file.getParent() == null) {
            throw new IOException(VfsBundle.message("cannot.rename.root.directory", new Object[]{file.getPath()}));
        } else if(newParent.exists() && newParent.isDirectory()) {
            if(newParent.findChild(name) != null) {
                throw new IOException(VfsBundle.message("vfs.target.already.exists.error", new Object[]{newParent.getPath() + "/" + name}));
            } else {
                File ioFile = convertToIOFile(file);
                if(FileSystemUtil.getAttributes(ioFile) == null) {
                    throw new FileNotFoundException(VfsBundle.message("file.not.exist.error", new Object[]{ioFile.getPath()}));
                } else {
                    File ioParent = convertToIOFile(newParent);
                    if(!ioParent.isDirectory()) {
                        throw new IOException(VfsBundle.message("target.not.directory.error", new Object[]{ioParent.getPath()}));
                    } else {
                        File ioTarget = new File(ioParent, name);
                        if(ioTarget.exists()) {
                            throw new IOException(VfsBundle.message("target.already.exists.error", new Object[]{ioTarget.getPath()}));
                        } else if(!this.auxMove(file, newParent) && !ioFile.renameTo(ioTarget)) {
                            throw new IOException(VfsBundle.message("move.failed.error", new Object[]{ioFile.getPath(), ioParent.getPath()}));
                        } else {
                            this.auxNotifyCompleted((handler) -> {
                                handler.move(file, newParent);
                            });
                        }
                    }
                }
            }
        } else {
            throw new IOException(VfsBundle.message("vfs.target.not.directory.error", new Object[]{newParent.getPath()}));
        }
    }

    public void renameFile(Object requestor, @NotNull VirtualFile file, @NotNull String newName) throws IOException {
        if(!this.isValidName(newName)) {
            throw new IOException(VfsBundle.message("file.invalid.name.error", new Object[]{newName}));
        } else {
            boolean sameName = !this.isCaseSensitive() && newName.equalsIgnoreCase(file.getName());
            if(!file.exists()) {
                throw new IOException(VfsBundle.message("vfs.file.not.exist.error", new Object[]{file.getPath()}));
            } else {
                VirtualFile parent = file.getParent();
                if(parent == null) {
                    throw new IOException(VfsBundle.message("cannot.rename.root.directory", new Object[]{file.getPath()}));
                } else if(!sameName && parent.findChild(newName) != null) {
                    throw new IOException(VfsBundle.message("vfs.target.already.exists.error", new Object[]{parent.getPath() + "/" + newName}));
                } else {
                    File ioFile = convertToIOFile(file);
                    if(!ioFile.exists()) {
                        throw new FileNotFoundException(VfsBundle.message("file.not.exist.error", new Object[]{ioFile.getPath()}));
                    } else {
                        File ioTarget = new File(convertToIOFile(parent), newName);
                        if(!sameName && ioTarget.exists()) {
                            throw new IOException(VfsBundle.message("target.already.exists.error", new Object[]{ioTarget.getPath()}));
                        } else if(!this.auxRename(file, newName) && !FileUtil.rename(ioFile, newName)) {
                            throw new IOException(VfsBundle.message("rename.failed.error", new Object[]{ioFile.getPath(), newName}));
                        } else {
                            this.auxNotifyCompleted((handler) -> {
                                handler.rename(file, newName);
                            });
                        }
                    }
                }
            }
        }
    }

    @NotNull
    public VirtualFile copyFile(Object requestor, @NotNull VirtualFile file, @NotNull VirtualFile newParent, @NotNull String copyName) throws IOException {
        if(!this.isValidName(copyName)) {
            throw new IOException(VfsBundle.message("file.invalid.name.error", new Object[]{copyName}));
        } else if(!file.exists()) {
            throw new IOException(VfsBundle.message("vfs.file.not.exist.error", new Object[]{file.getPath()}));
        } else if(newParent.exists() && newParent.isDirectory()) {
            if(newParent.findChild(copyName) != null) {
                throw new IOException(VfsBundle.message("vfs.target.already.exists.error", new Object[]{newParent.getPath() + "/" + copyName}));
            } else {
                FileAttributes attributes = this.getAttributes(file);
                if(attributes == null) {
                    throw new FileNotFoundException(VfsBundle.message("file.not.exist.error", new Object[]{file.getPath()}));
                } else if(attributes.isSpecial()) {
                    throw new FileNotFoundException("Not a file: " + file);
                } else {
                    File ioParent = convertToIOFile(newParent);
                    if(!ioParent.isDirectory()) {
                        throw new IOException(VfsBundle.message("target.not.directory.error", new Object[]{ioParent.getPath()}));
                    } else {
                        File ioTarget = new File(ioParent, copyName);
                        if(ioTarget.exists()) {
                            throw new IOException(VfsBundle.message("target.already.exists.error", new Object[]{ioTarget.getPath()}));
                        } else {
                            if(!this.auxCopy(file, newParent, copyName)) {
                                try {
                                    File e = convertToIOFile(file);
                                    FileUtil.copyFileOrDir(e, ioTarget, attributes.isDirectory());
                                } catch (IOException var9) {
                                    FileUtil.delete(ioTarget);
                                    throw var9;
                                }
                            }

                            this.auxNotifyCompleted((handler) -> {
                                handler.copy(file, newParent, copyName);
                            });
                            return new FakeVirtualFile(newParent, copyName);
                        }
                    }
                }
            }
        } else {
            throw new IOException(VfsBundle.message("vfs.target.not.directory.error", new Object[]{newParent.getPath()}));
        }
    }

    public void setTimeStamp(@NotNull VirtualFile file, long timeStamp) {
        File ioFile = convertToIOFile(file);
        if(ioFile.exists() && !ioFile.setLastModified(timeStamp)) {
            LOG.warn("Failed: " + file.getPath() + ", new:" + timeStamp + ", old:" + ioFile.lastModified());
        }

    }

    public void setWritable(@NotNull VirtualFile file, boolean writableFlag) throws IOException {
        String path = FileUtil.toSystemDependentName(file.getPath());
        FileUtil.setReadOnlyAttribute(path, !writableFlag);
        if(FileUtil.canWrite(path) != writableFlag) {
            throw new IOException("Failed to change read-only flag for " + path);
        }
    }

    @NotNull
    protected String extractRootPath(@NotNull String path) {
        if(path.isEmpty()) {
            String var10000;
            try {
                var10000 = this.extractRootPath((new File("")).getCanonicalPath());
            } catch (IOException var5) {
                throw new RuntimeException(var5);
            }

            return var10000;
        } else if(!SystemInfo.isWindows) {
            return StringUtil.startsWithChar(path, '/')?"/":"";
        } else if(path.length() >= 2 && path.charAt(1) == 58) {
            return path.substring(0, 2).toUpperCase(Locale.US);
        } else if(!path.startsWith("//") && !path.startsWith("\\\\")) {
            return "";
        } else {
            int slashCount = 0;

            int idx;
            for(idx = 2; idx < path.length() && slashCount < 2; ++idx) {
                char c = path.charAt(idx);
                if(c == 92 || c == 47) {
                    ++slashCount;
                    --idx;
                }
            }

            return path.substring(0, idx);
        }
    }

    public int getRank() {
        return 1;
    }

    public boolean markNewFilesAsDirty() {
        return true;
    }

    @NotNull
    public String getCanonicallyCasedName(@NotNull VirtualFile file) {
        if(this.isCaseSensitive()) {
            return super.getCanonicallyCasedName(file);
        } else {
            String originalFileName = file.getName();

            String var10000;
            try {
                File e = convertToIOFile(file);
                File ioCanonicalFile = e.getCanonicalFile();
                String canonicalFileName = ioCanonicalFile.getName();
                if(SystemInfo.isUnix) {
                    if(canonicalFileName.compareToIgnoreCase(originalFileName) == 0) {
                        var10000 = canonicalFileName;
                        return var10000;
                    }

                    File parentFile = e.getParentFile();
                    if(parentFile != null) {
                        String[] canonicalFileNames = parentFile.list();
                        if(canonicalFileNames != null) {
                            String[] var8 = canonicalFileNames;
                            int var9 = canonicalFileNames.length;

                            for(int var10 = 0; var10 < var9; ++var10) {
                                String name = var8[var10];
                                if(name.compareToIgnoreCase(originalFileName) == 0) {
                                    var10000 = name;
                                    return var10000;
                                }
                            }
                        }
                    }

                    var10000 = canonicalFileName;
                    return var10000;
                }

                var10000 = canonicalFileName;
            } catch (IOException var12) {
                return originalFileName;
            }

            return var10000;
        }
    }

    public FileAttributes getAttributes(@NotNull VirtualFile file) {
        String path = this.normalize(file.getPath());
        return path == null?null:(file.getParent() == null && path.startsWith("//")?FAKE_ROOT_ATTRIBUTES:FileSystemUtil.getAttributes(FileUtil.toSystemDependentName(path)));
    }

    public void refresh(boolean asynchronous) {
        RefreshQueue.getInstance().refresh(asynchronous, true, (Runnable)null, ManagingFS.getInstance().getRoots(this));
    }
}
