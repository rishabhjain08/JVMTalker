package api;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.EverythingGlobalScope;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by rishajai on 10/18/16.
 */
public class MySearchScope extends EverythingGlobalScope {

    private List<VirtualFile> filesInScope;

    public MySearchScope() {
        filesInScope = new LinkedList<>();
    }

    public boolean contains(@NotNull VirtualFile myFile) {
        for (VirtualFile file : filesInScope) {
            if (file.getUrl().equals(myFile.getUrl())) {
                return true;
            }
        }
        return false;
    }

    public void addFile(VirtualFile myFile) {
        boolean found = false;
        for (VirtualFile file : filesInScope) {
            if (file.getUrl().equals(myFile.getUrl())) {
                found = true;
            }
        }
        if (!found) {
            filesInScope.add(myFile);
        }
    }

}
