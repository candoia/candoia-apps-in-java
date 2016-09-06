package bugToSrcFileMapper.setting5;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VCSModule {
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };
	protected ArrayList<SVNCommit> revisions = new ArrayList<SVNCommit>();
	static {
		DAVRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
		FSRepositoryFactory.setup();
	}

	private SVNRepository repository = null;
	private SVNURL url;
	private ISVNAuthenticationManager authManager;
	private long lastSeenRevision = 1l;
	private long latestRevision = 0l;

	public VCSModule(final String url) {
		try {
			this.url = SVNURL.fromFile(new File(url));
			this.authManager = SVNWCUtil.createDefaultAuthenticationManager("", "");
			this.repository = SVNRepositoryFactory.create(this.url);
			this.repository.setAuthenticationManager(this.authManager);
			this.latestRevision = this.repository.getLatestRevision();
		} catch (final SVNException e) {
			e.printStackTrace();
		}

	}

	public static boolean isFixingRevision(String commitLog) {
		boolean isFixing = false;
		Pattern p;
		if (commitLog != null) {
			String tmpLog = commitLog.toLowerCase();
			for (int i = 0; i < fixingPatterns.length; i++) {
				String patternStr = fixingPatterns[i];
				p = Pattern.compile(patternStr);
				Matcher m = p.matcher(tmpLog);
				isFixing = m.find();
				if (isFixing) {
					break;
				}
			}
		}
		return isFixing;
	}

	public ArrayList<SVNCommit> getAllRevisions() {
		if (latestRevision < 1l)
			return revisions;

		try {
			final Collection<SVNLogEntry> logEntries = repository.log(new String[] { "" }, null, lastSeenRevision + 1l,
					latestRevision, true, true);

			for (final SVNLogEntry logEntry : logEntries) {
				final SVNCommit revision = new SVNCommit(repository, this, logEntry);
				revision.setId("" + logEntry.getRevision());
				if (logEntry.getAuthor() == null)
					revision.setCommitter(logEntry.getAuthor());
				else
					revision.setCommitter("anonymous");
				revision.setDate(logEntry.getDate());
				revision.setMessage(logEntry.getMessage());

				if (logEntry.getChangedPaths() != null && logEntry.getChangedPaths().size() > 0) {
					final HashMap<String, String> rChangedPaths = new HashMap<String, String>();
					final HashMap<String, String> rRemovedPaths = new HashMap<String, String>();
					final HashMap<String, String> rAddedPaths = new HashMap<String, String>();
					for (final Iterator changedPaths = logEntry.getChangedPaths().keySet().iterator(); changedPaths
							.hasNext();) {
						final SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths()
								.get(changedPaths.next());
						if (repository.checkPath(entryPath.getPath(), logEntry.getRevision()) == SVNNodeKind.FILE) {
							if (entryPath.getType() == SVNLogEntryPath.TYPE_DELETED)
								rRemovedPaths.put(entryPath.getPath(), entryPath.getCopyPath());
							else if (entryPath.getType() == SVNLogEntryPath.TYPE_ADDED)
								rAddedPaths.put(entryPath.getPath(), entryPath.getCopyPath());
							else
								rChangedPaths.put(entryPath.getPath(), entryPath.getCopyPath());
						}
					}
					revision.setChangedPaths(rChangedPaths);
					revision.setRemovedPaths(rRemovedPaths);
					revision.setAddedPaths(rAddedPaths);
				}
				this.revisions.add(revision);
			}
		} catch (final SVNException e) {
			e.printStackTrace();
		}
		return revisions;
	}

	public SVNRepository getRepository() {
		return this.repository;
	}
}
