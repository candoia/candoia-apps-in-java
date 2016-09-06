package setting5.convention_checker;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class VCSModule {
	protected ArrayList<SVNCommit> revisions = new ArrayList<SVNCommit>();
	static {
		DAVRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
		FSRepositoryFactory.setup();
	}

	private SVNRepository repository = null;
	private SVNURL url;

	private ISVNAuthenticationManager authManager;
	private SVNClientManager clientManager = null;

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

	public String getFileContent(String filePath, long revisionId, SVNProperties svnProperties,
			ByteArrayOutputStream os) {
		try {
			this.repository.getFile(filePath, revisionId, svnProperties, os);
			return os.toString();
		} catch (SVNException e) {
			e.printStackTrace();
		}
		return "";
	}

	public ArrayList<String> getAllFilesFromHead(ArrayList<String> results) {
		try {
			return listEntries(results, "", "");
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
