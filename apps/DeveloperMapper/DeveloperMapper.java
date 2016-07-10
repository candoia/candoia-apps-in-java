package developerMapper;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * A class for Checking the number of null checks in the repository.
 */
public class DeveloperMapper {
	private HashMap<String, Long> devMapper = new HashMap<String, Long>();

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		String repoPath = "";

		// path of the repository
		if (args.length < 1) {
			repoPath = "/Users/nmtiwari/Desktop/test/pagal/__clonedByBoa/compiler";
		} else {
			repoPath = args[0];
		}

		/*
		 * Creating an object of the class and creating a must exists repository
		 */
		DeveloperMapper git = new DeveloperMapper();
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repo = null;
		try {
			repo = builder.setGitDir(new File(repoPath + "/.git")).setMustExist(true).build();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// get all the commits in the repository
		ArrayList<RevCommit> revisions = git.getAllRevisions(repo);

		int totalRevs = revisions.size();
		int diffRevisions = 0;
		int totalNullChecks = 0;

		/*
		 * From here the repository should comare each commit with its previous
		 * commit to get the diffs and then find out if some null check was
		 * added or not.
		 */

		/*
		 * Because there are no previous commit for inital commit. We can safely
		 * avoid the analysis of initial commit.
		 */

		/*
		 * A loop for compairing all the commits with its previous commit.
		 */

		System.out.println(revisions.size());
		for (int i = totalRevs - 1; i > 0; i--) {
			RevCommit revisionOld = revisions.get(i);
			RevCommit revisionNew = revisions.get(i - 1);
			String developer = revisionNew.getCommitterIdent().getName();
			try {
				// get all the diffs of this commit from previous commit.
				List<DiffEntry> diffs = diffsBetweenTwoRevAndChangeTypes(repo, revisionNew, revisionOld);
				/*
				 * A loop for handling all the diffs fro this commit and
				 * previous commit.
				 */
				for (DiffEntry diff : diffs) {
					String packageName = "no package";
					packageName = git.getDev(repo, revisionNew.getId(), revisionOld.getId(), diff);
					if (packageName != null) {
						String key = packageName + "-" + developer;
						long count = git.devMapper.getOrDefault(key, (long) 0);
						git.devMapper.put(key, count + 1);
					}

				}
			} catch (RevisionSyntaxException | IOException | GitAPIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(i);
		}

		for (String key : git.devMapper.keySet()) {
			System.out.println(key.substring(0, key.indexOf('-')) + " " + key.substring(key.indexOf('-') + 1) + " "
					+ git.devMapper.get(key));
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
		System.out.println("Total null:" + totalNullChecks);
	}

	private static List<DiffEntry> diffsBetweenTwoRevAndChangeTypes(Repository repository, RevCommit cur,
																	RevCommit prev) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException,
			IOException, GitAPIException {
		List<DiffEntry> diffs = new ArrayList<>();
		try (ObjectReader reader = repository.newObjectReader()) {
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, prev.getTree());
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, cur.getTree());

			// finally get the list of changed files
			Git git = new Git(repository);
			diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
		}
		return diffs;
	}

	/*
	 * Reads a file from a commit id, if the file exists. Else throws
	 * IllegalStateException
	 */
	private static String readFile(Repository repository, ObjectId lastCommitId, String path)
			throws MissingObjectException, IncorrectObjectTypeException, IOException {
		// System.out.println("reading: " + path);
		RevWalk revWalk = new RevWalk(repository);
		RevCommit commit = revWalk.parseCommit(lastCommitId);
		// and using commit's tree find the path
		RevTree tree = commit.getTree();
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		treeWalk.setFilter(PathFilter.create(path));
		if (!treeWalk.next()) {
			throw new IllegalStateException(path);
		}
		ObjectId objectId = treeWalk.getObjectId(0);
		ObjectLoader loader = repository.open(objectId);

		InputStream in = loader.openStream();
		java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	/*
	 * A function to get all the commits from the given repos.
	 */
	public ArrayList<RevCommit> getAllRevisions(Repository repo) {
		ArrayList<RevCommit> revisions = new ArrayList<>();
		Git git = new Git(repo);
		Iterable<RevCommit> allRevisions = null;
		try {
			allRevisions = git.log().call();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (RevCommit rev : allRevisions) {
			revisions.add(rev);
		}
		return revisions;
	}

	/*
	 * Given a diff, current and previous commit ids, count the number of null
	 * checks.
	 *
	 * @diff: Current diff
	 *
	 * @oldComit: Previous commit id
	 *
	 * @newComit: Current commit id
	 *
	 * @rep: repository under consideration This function only considers the
	 * diffs with modified status. In all other cases, this is not a null fix.
	 * Diff status: Add : Adding a new file so not a bug fix for null check Diff
	 * status: Delete : Deleting a file will not be null check addition Diff
	 * status: Copy : Similar to add
	 */
	private String getDev(Repository rep, ObjectId lastCommitId, ObjectId oldCommit, DiffEntry diff) {
		String packageName = "";
		// HashMap<String, String> modPaths = getModifiedPaths(rep, logEntry);
		String oldPath = diff.getOldPath();
		String newPath = diff.getNewPath();
		int nullInOld = 0;
		int nullInNew = 0;

		try {
			packageName = getDev(rep, oldCommit, oldPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return packageName;
	}

	/*
	 * Given a AST computes null check
	 */

	/*
	 * Reads a file and computes the null checks.
	 */
	private String getDev(Repository rep, ObjectId lastCommitId, String path)
			throws MissingObjectException, IncorrectObjectTypeException, IOException {
		String fileContent;
		try {
			fileContent = readFile(rep, lastCommitId, path);
			ASTNode ast = createAst(fileContent);
			return getDev(ast);
		} catch (Exception e) {
			return "";
		}
	}

	private String getDev(ASTNode ast) {
		class PackageNameExtractor extends ASTVisitor {
			int numOfNullChecks = 0;
			String packageName = "";

			@Override
			public boolean visit(PackageDeclaration node) {
				packageName = node.getName().getFullyQualifiedName();
				return true;
			}

			public String getPackageName() {
				return packageName;
			}
		}
		PackageNameExtractor v = new PackageNameExtractor();
		ast.accept(v);
		return v.getPackageName();
	}

	/*
	 * Creates AST
	 */
	private ASTNode createAst(String fileContent) {
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(fileContent.toCharArray());
		parser.setCompilerOptions(options);
		ASTNode ast = parser.createAST(null);
		return ast;
	}
}
