package setting5.nullCheck;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.User;

/**
 * Created by nmtiwari on 7/9/16.
 */
public class ForgeModule {
	private static char[] pwd = null;
	public static String oaToken = null;

	private static class Lock {
	}

	public static void clone(String URL, String repoPath) throws SVNException {
		SVNURL svnurl = SVNURL.parseURIDecoded(URL);
		SVNRepository srcRepository = SVNRepositoryFactory.create(svnurl);
		srcRepository.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager());
		SVNClientManager ourClientManager = SVNClientManager.newInstance();
		ourClientManager.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager());
		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		updateClient.setIgnoreExternals(true);

		long latestRevision = srcRepository.getLatestRevision();
		if (updateClient.doCheckout(svnurl, new File(repoPath), SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY,
				true) == latestRevision) {
			ourClientManager.dispose();
		}
	}

	public static void delete(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
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
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		return reader.readLine();
	}

	/*
	 * This method reads the user password using java.util.Console service. In
	 * some IDE's like eclipse this service is not available and creation of
	 * console form the IDE always return null. In that very case password is
	 * read using normal BufferedReader and input is visible as the user types
	 * in. This is not bug but a feature not supported in some IDEs.
	 */
	public static char[] readPassword() {
		synchronized (ForgeModule.Lock.class) {
			if (pwd != null) {
				return pwd;
			} else {
				Console cnsl = null;
				char[] password = null;
				try {
					cnsl = System.console();
					if (cnsl != null) {
						password = cnsl.readPassword("Password: ");
					} else {
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
