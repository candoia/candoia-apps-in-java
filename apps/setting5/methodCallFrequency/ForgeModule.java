package setting5.methodCallFrequency;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * Created by nmtiwari on 7/9/16.
 */
public class ForgeModule {
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
}
