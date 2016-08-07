package setting1.bugFileMapper;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import br.ufpe.cin.groundhog.Project;
public class ForgeModule {
	private Project project;
	private static char[] pwd = null;
	public static String oaToken = null;

	public static boolean clone(String URL, String localpaths) throws IOException, GitAPIException {
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
