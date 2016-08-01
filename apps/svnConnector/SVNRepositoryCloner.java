/*
 * ====================================================================
 * Copyright (c) 2004-2007 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package svnConnector;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SVNRepositoryCloner {
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
