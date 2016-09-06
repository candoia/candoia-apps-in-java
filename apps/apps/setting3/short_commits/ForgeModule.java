package setting3.short_commits;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

public class ForgeModule {
    public static boolean clone(String URL, String localpaths)
            throws IOException, GitAPIException {
        File localPath = new File(localpaths);
        if (!localPath.exists())
            localPath.mkdir();
        try {
            Git result = Git.cloneRepository().setURI(URL).setDirectory(localPath).call();
            result.getRepository().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
