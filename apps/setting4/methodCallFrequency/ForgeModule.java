package setting4.methodCallFrequency;

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
    private List<Issue> _issues;
//    private List<Commit> _commits;
    private static char[] pwd = null;
    public static String oaToken = null;
	private static class Lock{}
    
    ForgeModule(String username, String projName) {
        Injector injector = Guice.createInjector(new SearchModule());
        User user = new User(username);
        this.project = new Project(user, projName);
        this._issues = new ArrayList<>();
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

    
    private static String readLine() throws IOException {
        if (System.console() != null) {
            return System.console().readLine();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        return reader.readLine();
    } 
    
    /*
     * This method reads the user password using java.util.Console service. In some IDE's like
     * eclipse this service is not available and creation of console form the IDE always return 
     * null. In that very case password is read using normal BufferedReader and input is visible
     * as the user types in. This is not bug but a feature not supported in some IDEs. 
     */
    public static char[] readPassword(){
        synchronized (ForgeModule.Lock.class){
            if(pwd != null){
                return pwd;
            }else{
                Console cnsl = null;
                char[] password = null;
                try {
                    cnsl = System.console();
                    if (cnsl != null) {
                        password = cnsl.readPassword("Password: ");
                    }else{
                    	return readLine().toCharArray();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                pwd = password;
                return password;
            }
        }
    }
}
