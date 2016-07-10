package gitConnector;

import br.ufpe.cin.groundhog.Commit;
import br.ufpe.cin.groundhog.Issue;
import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.User;
import br.ufpe.cin.groundhog.search.SearchModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nmtiwari on 7/9/16.
 */
public class Github {
    private SearchIssues searchIssues;
    private Project project;
    private List<Issue> _issues;
    private List<Commit> _commits;

    Github(String username, String projName) {
        Injector injector = Guice.createInjector(new SearchModule());
        this.searchIssues = injector.getInstance(SearchIssues.class);
        User user = new User(username);
        this.project = new Project(user, projName);
        this._issues = new ArrayList<>();
        this._commits = new ArrayList<>();
        System.out.println("username: size: =" + username.length());
    }

    public static boolean clone(String URL, String localpaths)
            throws IOException, GitAPIException {
        String url = URL;
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

    public static void delete(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    delete(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    List<Issue> get_Issues() {
        this._issues = this.searchIssues.getAllProjectIssues(this.project);
        this._issues = this.searchIssues.getAllProjectIssues(this.project);
        return this._issues;
    }

}
