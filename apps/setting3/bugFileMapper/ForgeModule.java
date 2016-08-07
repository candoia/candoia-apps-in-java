package setting3.bugFileMapper;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.google.inject.Guice;
import com.google.inject.Injector;

import br.ufpe.cin.groundhog.Issue;
import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.User;
import br.ufpe.cin.groundhog.search.SearchModule;

/**
 * Created by nmtiwari on 7/9/16.
 */
public class ForgeModule {
    private Project project;
    private static char[] pwd = null;
    public static String oaToken = null;
	private static class Lock{}
    
    ForgeModule(String username, String projName) {
        User user = new User(username);
        this.project = new Project(user, projName);
    }

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
