package com.intellij.debugger.engine;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;

import java.util.List;

/**
 * Created by rishajai on 10/18/16.
 */
public class MyPositionManager extends PositionManagerImpl {

    public MyPositionManager(DebugProcessImpl debugProcess) {
        super(debugProcess);
    }

//    @Override
    protected PsiFile getPsiFileByLocation(final Project project, final Location location) {
        if (location == null) {
            return null;
        }
        final ReferenceType refType = location.declaringType();
        if (refType == null) {
            return null;
        }

        // We should find a class no matter what
        // setAlternativeResolveEnabled is turned on here
        //if (DumbService.getInstance(project).isDumb()) {
        //  return null;
        //}

        final String originalQName = refType.name();
        final GlobalSearchScope searchScope = getDebugProcess().getSearchScope();
        PsiClass psiClass = DebuggerUtils.findClass(originalQName, project, searchScope); // try to lookup original name first
        if (psiClass == null) {
            int dollar = originalQName.indexOf('$');
            if (dollar > 0) {
                final String qName = originalQName.substring(0, dollar);
                psiClass = DebuggerUtils.findClass(qName, project, searchScope);
            }
        }

        if (psiClass != null) {
            PsiElement element = psiClass.getNavigationElement();
            // see IDEA-137167, prefer not compiled elements
            if (element instanceof PsiCompiledElement) {
                PsiElement fileElement = psiClass.getContainingFile().getNavigationElement();
                if (!(fileElement instanceof PsiCompiledElement)) {
                    element = fileElement;
                }
            }
            return element.getContainingFile();
        }
        else {
            // try to search by filename
            try {
                List<VirtualFile> vFiles = ((DebugProcessImpl) getDebugProcess()).getRegisteredFilesByName(refType.sourceName());
                for (VirtualFile vFile : vFiles) {
                    PsiFile file = getDebugProcess().getProject().getComponent(PsiManager.class).findFile(vFile);
                    if (file != null) {
                        if (file instanceof PsiJavaFile) {
                            for (PsiClass cls : ((PsiJavaFile) file).getClasses()) {
                                if (StringUtil.equals(originalQName, cls.getQualifiedName())) {
                                    return file;
                                }
                            }
                        }
                    }
                }
            }
            catch (AbsentInformationException ignore) {
            }
        }

        return null;
    }
}
