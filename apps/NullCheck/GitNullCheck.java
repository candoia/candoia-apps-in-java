package NullCheck;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
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

/*
 * A class for Checking the number of null checks in the repository.
 */
public class GitNullCheck {
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };

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
		GitNullCheck git = new GitNullCheck();
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repo = null;
		try {
			repo = builder.setGitDir(new File(repoPath + "/.git")).setMustExist(true).build();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// get all the commits in the repository
		ArrayList<RevCommit> revisions = git.getAllRevisions(repo);

		System.out.println(revisions.size());
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

		System.out.println(revisions.get(0).getFullMessage());
		for (int i = totalRevs - 1; i > 0; i--) {
			RevCommit revisionOld = revisions.get(i);
			RevCommit revisionNew = revisions.get(i - 1);
			try {
				// get all the diffs of this commit from previous commit.
				List<DiffEntry> diffs = diffsBetweenTwoRevAndChangeTypes(repo, revisionNew, revisionOld);
				/*
				 * A loop for handling all the diffs fro this commit and
				 * previous commit.
				 */
				for (DiffEntry diff : diffs) {
					if (git.isFixingCommit(revisionNew.getFullMessage())) {
						// count the added null checks.
						int nulls = git.countNullCheckAdditions(repo, revisionNew.getId(), revisionOld.getId(),
								diff);
						if(nulls > 0){
							System.out.println(revisionNew.getFullMessage() + " " + revisionNew.getCommitTime());
						}
						totalNullChecks += nulls; 
					}
				}
			} catch (RevisionSyntaxException | IOException | GitAPIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
		System.out.println("Total null:" + totalNullChecks);
	}

	/*
	 * A function to check if commit message is fixing one or not. This is based
	 * on pattern match.
	 */
	private boolean isFixingCommit(String commitLog) {
		boolean isFixing = false;
		Pattern p;
		if (commitLog != null) {
			String tmpLog = commitLog.toLowerCase();
			for (int i = 0; i < fixingPatterns.length; i++) {
				String patternStr = fixingPatterns[i];
				p = Pattern.compile(patternStr);
				Matcher m = p.matcher(tmpLog);
				isFixing = m.find();
				if (isFixing == true)
					break;
			}
		}
		return isFixing;
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
	 * diffs wmodified status. In all other cases, this is not a null fix. Diff
	 * status: Add : Adding a new file so not a bug fix for null check Diff
	 * status: Delete : Deleting a file will not be null check addition Diff
	 * status: Copy : Similar to add
	 */
	private int countNullCheckAdditions(Repository rep, ObjectId lastCommitId, ObjectId oldCommit, DiffEntry diff) {
		int numOfNullCheckAdds = 0;
		// HashMap<String, String> modPaths = getModifiedPaths(rep, logEntry);
		String oldPath = diff.getOldPath();
		String newPath = diff.getNewPath();
		int nullInOld = 0;
		int nullInNew = 0;

		if (diff.getChangeType() == ChangeType.DELETE || diff.getChangeType() == ChangeType.RENAME
				|| diff.getChangeType() == ChangeType.ADD || diff.getChangeType() == ChangeType.COPY) {
			return numOfNullCheckAdds;
		} else if (diff.getChangeType() == ChangeType.MODIFY) {
			try {
				nullInOld = countNullChecks(rep, oldCommit, oldPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				nullInNew = countNullChecks(rep, lastCommitId, newPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (nullInNew > nullInOld) {
				numOfNullCheckAdds++;
				System.out.println("Found: " + newPath + " " + lastCommitId + " " + numOfNullCheckAdds);
			}
		}
		return numOfNullCheckAdds;
	}

	/*
	 * Reads a file and computes the null checks.
	 */
	private int countNullChecks(Repository rep, ObjectId lastCommitId, String path)
			throws MissingObjectException, IncorrectObjectTypeException, IOException {
		String fileContent;
		try {
			fileContent = readFile(rep, lastCommitId, path);
			ASTNode ast = createAst(fileContent);
			return countNullChecks(ast);
		} catch (Exception e) {
			return 0;
		}
	}

	/*
	 * Given a AST computes null check
	 */
	private int countNullChecks(ASTNode ast) {
		class NullCheckConditionVisitor extends ASTVisitor {
			int numOfNullChecks = 0;

			@Override
			public boolean visit(ConditionalExpression node) {
				node.getExpression().accept(new NullCheckExpressionVisitor());
				return super.visit(node);
			}

			@Override
			public boolean visit(IfStatement node) {
				node.getExpression().accept(new NullCheckExpressionVisitor());
				return super.visit(node);
			}

			class NullCheckExpressionVisitor extends ASTVisitor {
				@Override
				public boolean visit(InfixExpression node) {
					if (node.getOperator() == Operator.EQUALS || node.getOperator() == Operator.NOT_EQUALS) {
						if (node.getRightOperand() instanceof NullLiteral
								|| node.getLeftOperand() instanceof NullLiteral)
							numOfNullChecks++;
					}
					return super.visit(node);
				}
			}
		}
		;
		NullCheckConditionVisitor v = new NullCheckConditionVisitor();
		ast.accept(v);
		return v.numOfNullChecks;
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

	/*
	 * Computes a list of Diffs between two revisions.
	 * 
	 * @repository : Repository of interest
	 * 
	 * @fromThisAndPrev: Revision number relative to head. 0 means head, 1
	 * parent of head and 2 parent of parent of head
	 */
	private static List<DiffEntry> diffsBetweenTwoRevAndChangeTypes(Repository repository, int fromThisAndPrev)
			throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException,
			GitAPIException {
		// ObjectId oldHead = repository.resolve("HEAD^^^^{tree}");
		// ObjectId head = repository.resolve("HEAD^{tree}");
		ObjectId head = repository.resolve("HEAD~" + fromThisAndPrev + "^{tree}");
		ObjectId oldHead = repository.resolve("HEAD~" + (fromThisAndPrev - 1) + "^{tree}");
		List<DiffEntry> diffs = new ArrayList<>();
		// prepare the two iterators to compute the diff between
		try (ObjectReader reader = repository.newObjectReader()) {
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, oldHead);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, head);

			// finally get the list of changed files
			Git git = new Git(repository);
			diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
		}
		return diffs;
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
	 * A function which is not used in the code but it can be used to get
	 * modified paths.
	 */
	private HashMap<String, String> getModifiedPaths(List<DiffEntry> diffs) {
		HashMap<String, String> modPaths = new HashMap<String, String>();
		HashMap<String, String> rDirPaths = new HashMap<String, String>();
		int numOfModFiles = 0;
		for (DiffEntry diff : diffs) {
			String changedPath = diff.getOldPath();
			if (changedPath.endsWith(".java")) {
				if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY)
					numOfModFiles++;
			} else {
				File file = new File(changedPath);
				if (file.isDirectory()) {
					String entryPath = diff.getNewPath();
					if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
						String fromPath = entryPath;
						if (fromPath != null) {
							rDirPaths.put(changedPath, fromPath);
						}
					}
				}
			}
		}
		if (numOfModFiles == 0)
			return modPaths;
		for (DiffEntry diff : diffs) {
			String changedPath = diff.getOldPath();
			if (changedPath.endsWith(".java")) {
				String entryPath = diff.getNewPath();
				if (diff.getChangeType() != DiffEntry.ChangeType.DELETE) {
					if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY) {
						String fromPath = entryPath;
						if (fromPath == null) {
							for (String rDirPath : rDirPaths.keySet()) {
								if (changedPath.startsWith(rDirPath)) {
									fromPath = rDirPaths.get(rDirPath) + changedPath.substring(rDirPath.length());
									break;
								}
							}
							if (fromPath == null)
								fromPath = changedPath;
						}
						modPaths.put(changedPath, fromPath);
					}
				}
			}
		}
		return modPaths;
	}

}
