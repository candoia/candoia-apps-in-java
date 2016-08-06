package settging1.fileAssociationMining;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Created by nmtiwari on 7/9/16. This is class for handling git connections and
 * cloning from repo
 */
public class VCSModule {
	// patters to check if fixings
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };
	private FileRepositoryBuilder builder;
	private Repository repository;
	private Git git;
	private String path;
	// private String userName;
	// private String projName;

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

	// clone the repository from remote at given local path
	public static boolean cloneRepo(String URL, String repoPath) {
		// String url = URL.substring(URL.indexOf('@') + 1, URL.length()) +
		// ".git";
		try {
			ForgeModule.clone(URL, repoPath);
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		}
		return false;
	}

	public Repository getRepository() {
		return this.repository;
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
	 * @cur: Current revision
	 * 
	 * @prev: previsous revision Returns a list of DiffEntries from the current
	 * and previous revision
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
	 * @repository: Git Repository
	 * 
	 * @commit: Revsion id returns a revision commit version of the revision id
	 */
	public RevCommit buildRevCommit(Repository repository, String commit) throws IOException {
		// a RevWalk allows to walk over commits based on some filtering that is
		// defined
		RevWalk revWalk = new RevWalk(repository);
		return revWalk.parseCommit(ObjectId.fromString(commit));
	}

	/*
	 * @repository: Git repository
	 * 
	 * @commit: revision id Returns list of file paths from this revision of
	 * given repository
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
}
