//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package api;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MyCoreJavaFileManager implements JavaFileManager {
    private static final Logger LOG = Logger.getInstance("#com.intellij.core.CoreJavaFileManager");
    private final List<VirtualFile> myClasspath = new ArrayList();
    private final PsiManager myPsiManager;

    public MyCoreJavaFileManager(PsiManager psiManager) {
        this.myPsiManager = psiManager;
    }

    private List<VirtualFile> roots() {
        return this.myClasspath;
    }

    public PsiPackage findPackage(@NotNull String packageName) {
        List files = this.findDirectoriesByPackageName(packageName);
        return !files.isEmpty()?new PsiPackageImpl(this.myPsiManager, packageName):null;
    }

    private List<VirtualFile> findDirectoriesByPackageName(String packageName) {
        ArrayList result = new ArrayList();
        String dirName = packageName.replace(".", "/");
        Iterator var4 = this.roots().iterator();

        while(var4.hasNext()) {
            VirtualFile root = (VirtualFile)var4.next();
            VirtualFile classDir = root.findFileByRelativePath(dirName);
            if(classDir != null) {
                result.add(classDir);
            }
        }

        return result;
    }

    @Nullable
    public PsiPackage getPackage(PsiDirectory dir) {
        VirtualFile file = dir.getVirtualFile();
        Iterator var3 = this.myClasspath.iterator();

        while(var3.hasNext()) {
            VirtualFile root = (VirtualFile)var3.next();
            if(VfsUtilCore.isAncestor(root, file, false)) {
                String relativePath = FileUtil.getRelativePath(root.getPath(), file.getPath(), '/');
                if(relativePath != null) {
                    return new PsiPackageImpl(this.myPsiManager, relativePath.replace('/', '.'));
                }
            }
        }

        return null;
    }

    public PsiClass findClass(@NotNull String qName, @NotNull GlobalSearchScope scope) {
        Iterator var3 = this.roots().iterator();

        PsiClass psiClass;
        do {
            if(!var3.hasNext()) {
                return null;
            }

            VirtualFile root = (VirtualFile)var3.next();
            psiClass = findClassInClasspathRoot(qName, root, this.myPsiManager, scope);
        } while(psiClass == null);

        return psiClass;
    }

    @Nullable
    public static PsiClass findClassInClasspathRoot(@NotNull String qName, @NotNull VirtualFile root, @NotNull PsiManager psiManager, @NotNull GlobalSearchScope scope) {
        String pathRest = qName;
        VirtualFile cur = root;

        String topLevelClassName;
        VirtualFile vFile;
        while(true) {
            int classNameWithInnerClasses = pathRest.indexOf(46);
            if(classNameWithInnerClasses < 0) {
                break;
            }

            topLevelClassName = pathRest.substring(0, classNameWithInnerClasses);
            vFile = cur.findChild(topLevelClassName);
            if(vFile == null) {
                break;
            }

            pathRest = pathRest.substring(classNameWithInnerClasses + 1);
            cur = vFile;
        }

        topLevelClassName = substringBeforeFirstDot(pathRest);
        vFile = cur.findChild(topLevelClassName + ".class");
        if(vFile == null) {
            vFile = cur.findChild(topLevelClassName + ".java");
        }

        if(vFile == null) {
            return null;
        } else if(!vFile.isValid()) {
            LOG.error("Invalid child of valid parent: " + vFile.getPath() + "; " + root.isValid() + " path=" + root.getPath());
            return null;
        } else if(!scope.contains(vFile)) {
            return null;
        } else {
            PsiFile file = psiManager.findFile(vFile);
            return !(file instanceof PsiClassOwner)?null:findClassInPsiFile(pathRest, (PsiClassOwner)file);
        }
    }

    @NotNull
    private static String substringBeforeFirstDot(@NotNull String classNameWithInnerClasses) {
        int dot = classNameWithInnerClasses.indexOf(46);
        return dot < 0?classNameWithInnerClasses:classNameWithInnerClasses.substring(0, dot);
    }

    @Nullable
    private static PsiClass findClassInPsiFile(@NotNull String classNameWithInnerClassesDotSeparated, @NotNull PsiClassOwner file) {
        PsiClass[] var2 = file.getClasses();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            PsiClass topLevelClass = var2[var4];
            PsiClass candidate = findClassByTopLevelClass(classNameWithInnerClassesDotSeparated, topLevelClass);
            if(candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    @Nullable
    private static PsiClass findClassByTopLevelClass(@NotNull String className, @NotNull PsiClass topLevelClass) {
        if(className.indexOf(46) < 0) {
            return className.equals(topLevelClass.getName())?topLevelClass:null;
        } else {
            Iterator segments = StringUtil.split(className, ".").iterator();
            if(segments.hasNext() && ((String)segments.next()).equals(topLevelClass.getName())) {
                PsiClass curClass;
                PsiClass innerClass;
                for(curClass = topLevelClass; segments.hasNext(); curClass = innerClass) {
                    String innerClassName = (String)segments.next();
                    innerClass = curClass.findInnerClassByName(innerClassName, false);
                    if(innerClass == null) {
                        return null;
                    }
                }

                return curClass;
            } else {
                return null;
            }
        }
    }

    @NotNull
    public PsiClass[] findClasses(@NotNull String qName, @NotNull GlobalSearchScope scope) {
        ArrayList result = new ArrayList();
        Iterator var4 = this.roots().iterator();

        while(var4.hasNext()) {
            VirtualFile file = (VirtualFile)var4.next();
            PsiClass psiClass = findClassInClasspathRoot(qName, file, this.myPsiManager, scope);
            if(psiClass != null) {
                result.add(psiClass);
            }
        }

        return (PsiClass[])result.toArray(new PsiClass[result.size()]);
    }

    @NotNull
    public Collection<String> getNonTrivialPackagePrefixes() {
        return Collections.emptyList();
    }

    public void addToClasspath(VirtualFile root) {
        boolean found = false;
        for (VirtualFile file : this.myClasspath) {
            if (file.getUrl().equals(root.getUrl())) {
                found = true;
                break;
            }
        }
        if (!found) {
            this.myClasspath.add(root);
        }
    }
}
