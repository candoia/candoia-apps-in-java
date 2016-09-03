package nullcheck.setting4;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nmtiwari on 7/9/16. A class to check all the Null Checks in the
 * subjected repository.
 */
public class Mining {
	private VCSModule git;
	private String userName;
	private String projName;
	private String bugURL;
	private String product;

	private Mining(String repoPath, String bug_url) {
		this.git = new VCSModule(repoPath);
		String[] details = repoPath.split("/");
		this.projName = details[details.length - 1];
		this.userName = details[details.length - 2];
		this.bugURL = bug_url.substring(bug_url.indexOf('@') + 1);
		this.product = bug_url.substring(0, bug_url.indexOf('@'));
	}
	private Mining(String url, String path, String bug_url) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		if (new File(path).isDirectory()) {
			try {
				ForgeModule.clone(url, path);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
		}

		this.git = new VCSModule(path);
		this.bugURL = bug_url.substring(bug_url.indexOf('@') + 1);
		this.product = bug_url.substring(0, bug_url.indexOf('@'));
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		Mining nullCheck = null;
		if (args.length == 3) {
			nullCheck = new Mining(args[1], args[0], args[2]);
		} else {
			nullCheck = new Mining("/Users/nmtiwari/Desktop/test/pagal/__clonedByBoa/boalang/compiler",
					"SLF4J@http://jira.qos.ch/");
		}

		ArrayList<RevCommit> revisions = nullCheck.git.getAllRevisions();
		ArrayList<RevCommit> nullFixingRevs = new ArrayList<RevCommit>();
		ArrayList<RevCommit> fixingRevs = new ArrayList<RevCommit>();
		BugModule bugs = new BugModule();
		int totalRevs = revisions.size();
		List<SVNTicket> issues = new ArrayList<>();
		System.out.println(nullCheck.bugURL + "\n" + nullCheck.product);
		try {
			issues = bugs.getIssues(nullCheck.bugURL, nullCheck.product);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Revisions and Issues: " + totalRevs + " " + issues.size());
		for (int i = totalRevs - 1; i > 0; i--) {
			RevCommit revisionOld = revisions.get(i);
			RevCommit revisionNew = revisions.get(i - 1);
			try {
				List<DiffEntry> diffs = nullCheck.git.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
				String commitMsg = revisionNew.getFullMessage();
				if (nullCheck.git.isFixingRevision(commitMsg)) {
					fixingRevs.add(revisionNew);
				}
				for (DiffEntry diff : diffs) {
					if (bugs.isFixingRevision(commitMsg, issues)) {
						int count = nullCheck.countNullCheckAdditions(revisionNew.getId(), revisionOld.getId(), diff);
						if (count > 0) {
							nullFixingRevs.add(revisionNew);
						}
					}
				}
			} catch (RevisionSyntaxException | IOException | GitAPIException e) {
				e.printStackTrace();
			}
		}
		long endTime = System.currentTimeMillis();
		HashMap<String, Integer> result = new HashMap<>();
		result.put("total revs", totalRevs);
		result.put("fixing revisions", fixingRevs.size());
		result.put("Null fixing revisions", nullFixingRevs.size());
		Visualization.saveGraph(result, "/Users/nmtiwari/Desktop/null.html");
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}

	private int countNullCheckAdditions(ObjectId lastCommitId, ObjectId oldCommit, DiffEntry diff) {
		int numOfNullCheckAdds = 0;
		String oldPath = diff.getOldPath();
		String newPath = diff.getNewPath();
		int nullInOld = 0;
		int nullInNew = 0;
		if (diff.getChangeType() == DiffEntry.ChangeType.DELETE || diff.getChangeType() == DiffEntry.ChangeType.RENAME
				|| diff.getChangeType() == DiffEntry.ChangeType.ADD
				|| diff.getChangeType() == DiffEntry.ChangeType.COPY) {
			return numOfNullCheckAdds;
		} else if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY) {
			try {
				nullInOld = countNullChecks(oldCommit, oldPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				nullInNew = countNullChecks(lastCommitId, newPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (nullInNew > nullInOld) {
				// numOfNullCheckAdds++;
				numOfNullCheckAdds = numOfNullCheckAdds + (nullInNew - nullInOld);
			}
		}
		return numOfNullCheckAdds;
	}

	private int countNullChecks(ObjectId lastCommitId, String path) throws IOException {
		String fileContent;
		try {
			fileContent = this.git.readFile(lastCommitId, path);
			ASTNode ast = this.git.createAst(fileContent);
			return countNullChecks(ast);
		} catch (Exception e) {
			return 0;
		}
	}

	private int countNullChecks(ASTNode ast) {
		class NullCheckConditionVisitor extends ASTVisitor {
			private int numOfNullChecks = 0;

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
					if (node.getOperator() == InfixExpression.Operator.EQUALS
							|| node.getOperator() == InfixExpression.Operator.NOT_EQUALS) {
						if (node.getRightOperand() instanceof NullLiteral
								|| node.getLeftOperand() instanceof NullLiteral)
							numOfNullChecks++;
					}
					return super.visit(node);
				}
			}
		}
		NullCheckConditionVisitor v = new NullCheckConditionVisitor();
		ast.accept(v);
		return v.numOfNullChecks;
	}
}