package setting6.nullCheck;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.dom.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import br.ufpe.cin.groundhog.Issue;

public class VCSModule {
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };
	private FileRepositoryBuilder builder;
	private Repository repository;
	private Git git;
	private String path;

	public VCSModule(String path) {
		this.builder = new FileRepositoryBuilder();
		this.path = path;
		try {
			this.repository = this.builder.setGitDir(new File(path + "/.git")).setMustExist(true).build();
			this.git = new Git(this.repository);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	public Repository getRepository() {
		return this.repository;
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

	public ArrayList<RevCommit> getAllRevisions() {
		ArrayList<RevCommit> revisions = new ArrayList<>();
		Iterable<RevCommit> allRevisions = null;
		try {
			allRevisions = git.log().call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		for (RevCommit rev : allRevisions) {
			revisions.add(rev);
		}
		return revisions;
	}

	public ASTNode createAst(String fileContent) {
		Map<String, String> options = JavaScriptCore.getOptions();
		options.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_5);
		options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_5);
		options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_5);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(fileContent.toCharArray());
		parser.setCompilerOptions(options);
		ASTNode ast = parser.createAST(null);
		return ast;
	}

	public List<DiffEntry> diffsBetweenTwoRevAndChangeTypes(RevCommit cur, RevCommit prev)
			throws RevisionSyntaxException, IOException, GitAPIException {
		List<DiffEntry> diffs = new ArrayList<>();
		ObjectReader reader = this.repository.newObjectReader();
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset(reader, prev.getTree());
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset(reader, cur.getTree());
		diffs = this.git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
		return diffs;
	}

	public String readFile(ObjectId lastCommitId, String path) throws IOException {
		RevWalk revWalk = new RevWalk(this.repository);
		RevCommit commit = revWalk.parseCommit(lastCommitId);
		RevTree tree = commit.getTree();
		TreeWalk treeWalk = new TreeWalk(this.repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		treeWalk.setFilter(PathFilter.create(path));
		if (!treeWalk.next()) {

			throw new IllegalStateException(path);
		}
		ObjectId objectId = treeWalk.getObjectId(0);
		ObjectLoader loader = this.repository.open(objectId);
		InputStream in = loader.openStream();
		java.util.Scanner s = new java.util.Scanner(in);
		s.useDelimiter("\\A");
		String result = s.hasNext() ? s.next() : "";
		s.close();
		in.close();
		return result;
	}

	public List<Integer> getIdsFromCommitMsg(String commitLog) {
		String commitMsg = commitLog;
		commitMsg = commitMsg.replaceAll("[^0-9]+", " ");
		List<String> idAsString = Arrays.asList(commitMsg.trim().split(" "));
		List<Integer> ids = new ArrayList<Integer>();
		for (String id : idAsString) {
			try {
				if (!ids.contains(Integer.parseInt(id)))
					ids.add(Integer.parseInt(id));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return ids;
	}

	public List<Integer> getIssueNumbers(List<Issue> issues) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Issue issue : issues) {
			ids.add(issue.getNumber());
		}
		return ids;
	}

	public boolean isFixingRevision(String msg, List<Issue> issues) {
		if (isFixingRevision(msg)) {
			List<Integer> ids = getIssueNumbers(issues);
			List<Integer> bugs = getIdsFromCommitMsg(msg);
			for (Integer i : bugs) {
				if (ids.contains(i)) {
					return true;
				}
			}
		}
		return false;
	}
}
