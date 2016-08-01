package gitConnector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
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

/**
 * Created by nmtiwari on 7/9/16.
 * This is class for handling git connections and cloning from repo
 */
public class GitConnector {
	// patters to check if fixings
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };
	private FileRepositoryBuilder builder;
	private Repository repository;
	private Git git;
	private String path;
	// private String userName;
	// private String projName;

	public GitConnector(String path) {
		this.builder = new FileRepositoryBuilder();
		this.path = path;
		try {
			this.repository = this.builder.setGitDir(new File(path + "/.git")).setMustExist(true).build();
			this.git = new Git(this.repository);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	// clone the repository from remote at given local path
	public static boolean cloneRepo(String URL, String repoPath) {
		// String url = URL.substring(URL.indexOf('@') + 1, URL.length()) +
		// ".git";
		try {
			Github.clone(URL, repoPath);
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		}
		return false;
	}

	public Repository getRepository() {
		return this.repository;
	}

	/*
	 * @commitLog: commit message
	 * returns boolean
	 * Checks if the revision has any of the fixing patterns
	 */
	public boolean isFixingRevision(String commitLog) {
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

	/*
	 * A function to get all the revisions of the repository
	 */
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

	/*
	 * @fileContent: A file content as string
	 * returns AST of the content using Java JDT.
	 */
	public ASTNode createAst(String fileContent) {
		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(fileContent.toCharArray());
		parser.setCompilerOptions(options);
		ASTNode ast = parser.createAST(null);
		return ast;
	}

	/*
 	 * @cur: Current revision
 	 * @prev: previsous revision
 	 * Returns a list of DiffEntries from the current and previous revision
 	*/
	public List<DiffEntry> diffsBetweenTwoRevAndChangeTypes(RevCommit cur, RevCommit prev)
			throws RevisionSyntaxException, IOException, GitAPIException {
		List<DiffEntry> diffs = new ArrayList<>();
		ObjectReader reader = this.repository.newObjectReader();
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset(reader, prev.getTree());
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset(reader, cur.getTree());

		// finally get the list of changed files
		diffs = this.git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
		return diffs;
	}

	/*
	 * Reads a file from a commit id, if the file exists. Else throws
	 * IllegalStateException
	 */
	public String readFile(ObjectId lastCommitId, String path) throws IOException {
		// System.out.println("reading: " + path);
		RevWalk revWalk = new RevWalk(this.repository);
		RevCommit commit = revWalk.parseCommit(lastCommitId);
		// and using commit's tree find the path
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

	/*
	 * @username: Repository username
     * @projName: project name
     * retuns a list of issues from the git tickets using the username and projName
     */
	public List<Issue> getIssues(String username, String projName) {
		Github git = new Github(username, projName);
		List<Issue> issues = git.get_Issues();
		return issues;
	}

	/*
	 * A simple method which fetches all the numbers from the string.
	 * Note: It does not verify if the numbers are real bug ids or not.
	 */
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
				// e.printStackTrace();
			}
		}
		return ids;
	}

	/*
	 * A method to get all issue ids for given username and projName.
	 */
	public List<Integer> getIssueIds(String username, String projName) {
		Github git = new Github(username, projName);
		List<Issue> issues = git.get_Issues();
		List<Integer> ids = new ArrayList<Integer>();
		for (Issue issue : issues) {
			ids.add(issue.getId());
		}
		return ids;
	}

	/*
	 * A overloaded version of getIssueIds
	 */
	public List<Integer> getIssueIds(List<Issue> issues) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Issue issue : issues) {
			ids.add(issue.getId());
		}
		return ids;
	}

	/*
	 * A method to get a list of issue numbers. Issue number is different than issue id.
     */
	public List<Integer> getIssueNumbers(List<Issue> issues) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Issue issue : issues) {
			ids.add(issue.getNumber());
		}
		return ids;
	}

	/*
	 * @msg: COmmit message
	 * @issues: list of all issues
	 * return boolean if this msg contains any real bug id or not
	 */
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

	/*
	 * @log: commit message
	 * @issues: list of all issues
	 * returns a list of integers representing issue numbers.
	 * This method gives you actual issue numbers.
	 */
	public List<Integer> getIssueIDsFromCommitLog(String log, List<Issue> issues) {
		List<Integer> ids = getIdsFromCommitMsg(log);
		List<Integer> bugs = new ArrayList<>();
		for (Integer i : ids) {
			if (isBug(issues, i)) {
				bugs.add(i);
			}
		}
		return bugs; 
	}

	/*
	 * @repository: Git repository
	 * @commit: revision id
	 * Returns list of file paths from this revision of given repository
	 */
	public List<String> readElementsAt(Repository repository, String commit) throws IOException {
		RevCommit revCommit = buildRevCommit(repository, commit);

		// and using commit's tree find the path
		RevTree tree = revCommit.getTree();
		// System.out.println("Having tree: " + tree + " for commit " + commit);

		List<String> items = new ArrayList<>();

		// shortcut for root-path
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		treeWalk.setPostOrderTraversal(true);

		while (treeWalk.next()) {
			items.add(treeWalk.getPathString());
		}
		return items;
	}

	/*
	 * @repository: Git Repository
	 * @commit: Revsion id
	 * returns a revision commit version of the revision id
	 */
	public RevCommit buildRevCommit(Repository repository, String commit) throws IOException {
		// a RevWalk allows to walk over commits based on some filtering that is
		// defined
		RevWalk revWalk = new RevWalk(repository);
		return revWalk.parseCommit(ObjectId.fromString(commit));

	}

	/*
	 * A method to get all the files from the head version. It is kind of
     * getting latest revision of the repository.
     */
	public List<String> getAllFilesFromHead() {
		ArrayList<String> results = new ArrayList<>();
		try {
			Ref head = this.repository.getRef("HEAD");
			RevWalk walk = new RevWalk(this.repository);
			RevCommit revision = walk.parseCommit(head.getObjectId());
			RevTree tree = revision.getTree();
			TreeWalk walker = new TreeWalk(this.repository);
			walker.addTree(tree);
			walker.setRecursive(true);
//		Can set a filter to read all java files only
//		walker.setFilter(PathFilter.create("README.md"));
			while (walker.next()) {
				results.add(walker.getPathString());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return results;
	}

	public List<String> getAllFilesFromHeadWithAbsPath() {
		ArrayList<String> results = new ArrayList<>();
		try {
			Ref head = this.repository.getRef("HEAD");
			RevWalk walk = new RevWalk(this.repository);
			RevCommit revision = walk.parseCommit(head.getObjectId());
			RevTree tree = revision.getTree();
			TreeWalk walker = new TreeWalk(this.repository);
			walker.addTree(tree);
			walker.setRecursive(true);
//		Can set a filter to read all java files only
//		walker.setFilter(PathFilter.create("README.md"));
			while (walker.next()) {
				results.add(this.path + "/" + walker.getPathString());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return results;
	}

	/*
	 * @issues: List of all github issues
	 * @id: integer
	 * returns if id is actual bug id or not
	 */
	private boolean isBug(List<Issue> issues, int id) {
		for (Issue issue : issues) {
			if (id == issue.getNumber()) {
				return true;
			}
		}
		return false;
	}
}
