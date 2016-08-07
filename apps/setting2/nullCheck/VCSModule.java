package setting2.nullCheck;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import br.ufpe.cin.groundhog.http.Requests;

/**
 * @author hoan
 * @author rdyer
 */
public class VCSModule {
	protected ArrayList<SVNCommit> revisions = new ArrayList<SVNCommit>();
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };
	static {
		// For using over http:// and https://
		DAVRepositoryFactory.setup();
		// For using over svn:// and svn+xxx://
		SVNRepositoryFactoryImpl.setup();
		// For using over file:///
		FSRepositoryFactory.setup();
	}

	private SVNRepository repository = null;
	private SVNURL url;

	private ISVNAuthenticationManager authManager;
	private SVNClientManager clientManager = null;

	private long lastSeenRevision = 1l;
	private long latestRevision = 0l;

	public VCSModule(final String url) {
		this(url, "", "");
	}

	public VCSModule() {
		this.url = null;
		this.authManager = null;
		this.repository = null;
	}

	// clone the repository from remote at given local path
	public static boolean cloneRepo(String URL, String repoPath) {
		// String url = URL.substring(URL.indexOf('@') + 1, URL.length()) +
		// ".git";
		try {
			ForgeModule.clone(URL, repoPath);
		} catch (SVNException e) {
			e.printStackTrace();
		}
		return false;
	}

	public VCSModule(final String url, final String username, final String password) {
		try {
			this.url = SVNURL.fromFile(new File(url));

			this.authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);

			this.repository = SVNRepositoryFactory.create(this.url);
			this.repository.setAuthenticationManager(this.authManager);

			this.latestRevision = this.repository.getLatestRevision();
		} catch (final SVNException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		repository.closeSession();
	}

	public boolean clear() {
		this.close();
		return true;
	}

	public boolean initialize(String path) {
		try {
			this.url = SVNURL.parseURIEncoded("file:///" + path);
			this.authManager = SVNWCUtil.createDefaultAuthenticationManager("", "");
			this.repository = SVNRepositoryFactory.create(this.url);
			this.repository.setAuthenticationManager(this.authManager);
			this.latestRevision = this.repository.getLatestRevision();
		} catch (final SVNException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public String getLastCommitId() {
		if (latestRevision == 0l)
			return null;
		return "" + latestRevision;
	}

	public void setLastSeenCommitId(final String id) {
		lastSeenRevision = Long.parseLong(id);
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

	public void getTags(final List<String> names, final List<String> commits) {
		// TODO
	}

	public void getBranches(final List<String> names, final List<String> commits) {
		// TODO
	}

	/*
	 * @fileContent: A file content as string returns AST of the content using
	 * Java JDT.
	 */
	public ASTNode createAst(String fileContent) {
		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(fileContent.toCharArray());
		parser.setCompilerOptions(options);
		try {
			ASTNode ast = parser.createAST(null);
			return ast;
		} catch (Exception e) {
			parser.setSource(" ".toCharArray());
			ASTNode ast = parser.createAST(null);
			return ast;
		}
	}

	public SVNRepository getRepository() {
		return this.repository;
	}

	public ArrayList<SVNLogEntry> diffsBetweenTwoRevAndChangeTypes(SVNCommit revisionNew, SVNCommit revisionOld) {
		try {
			repository.setAuthenticationManager(authManager);
			Collection logEntries = new ArrayList<>();
			ArrayList<SVNLogEntry> result = new ArrayList<>();
			logEntries = repository.log(new String[] { "" }, null, revisionOld.getId(), revisionNew.getId(), true,
					true);

			for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
				SVNLogEntry logEntry = (SVNLogEntry) entries.next();
				result.add(logEntry);
				// System.out.println(String.format("revision: %d, date %s",
				// logEntry.getRevision(), logEntry.getDate()));
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public String getFileContent(String filePath, long revisionId, SVNProperties svnProperties,
			ByteArrayOutputStream os) {
		try {
			this.repository.getFile(filePath, revisionId, svnProperties, os);
			return os.toString();
		} catch (SVNException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return "";
	}

	public ArrayList<String> getAllFilesFromHead(ArrayList<String> results) {
		try {
			return listEntries(results, "", "");
			// return listEntries(results, "", this.url.getPath()+"/");
		} catch (SVNException e) {
			e.printStackTrace();
		}
		return results;
	}

	private ArrayList<String> listEntries(ArrayList<String> results, String path, String rootPath) throws SVNException {
		Collection entries = this.repository.getDir(path, -1, new SVNProperties(), (Collection) null);
		Iterator iterator = entries.iterator();
		while (iterator.hasNext()) {
			SVNDirEntry entry = (SVNDirEntry) iterator.next();
			if (entry.getKind() == SVNNodeKind.FILE) {
				results.add((path.equals("")) ? rootPath + entry.getName() : rootPath + path + "/" + entry.getName());
			}
			if (entry.getKind() == SVNNodeKind.DIR) {
				listEntries(results, (path.equals("")) ? entry.getName() : path + "/" + entry.getName(), rootPath);
			}
		}
		return results;
	}
}
