package setting3.bugFileMapper;

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
 * Created by nmtiwari on 7/9/16. This is class for handling git connections and
 * cloning from repo
 */
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

	public static boolean cloneRepo(String URL, String repoPath) {
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

	public List<String> readElementsAt(Repository repository, String commit) throws IOException {
		RevWalk revWalk = new RevWalk(repository);
		RevCommit revCommit = revWalk.parseCommit(ObjectId.fromString(commit));
		RevTree tree = revCommit.getTree();
		List<String> items = new ArrayList<>();
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
