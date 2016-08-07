package setting5.nullCheck;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNProperties;

/**
 * Created by nmtiwari on 7/9/16. A class to check all the Null Checks in the
 * subjected repository.
 */
public class Mining {
	private String userName;
	private String projName;
	private VCSModule svn;
	private String url;
	private String product;

	/*
	 * url must be of form: username@url
	 */
	private Mining(String url, String path) {
		this.url = url;
		this.userName = url.substring(0, url.indexOf('@'));
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		if(!new File(path).isDirectory())
		  VCSModule.cloneRepo(url.substring(url.indexOf('@') + 1), path);
		this.svn = new VCSModule(path);
	}

	/*
	 * Main function for NullCheckGit_GIT_Ticket
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		Mining nullCheck = null;
		// path of the repository
		if (args.length == 2) {
			nullCheck = new Mining(args[0], args[1]);
		} else {
			throw new IllegalArgumentException();
		}

		ArrayList<SVNCommit> revisions = nullCheck.svn.getAllRevisions();
		ArrayList<SVNCommit> nullFixingRevs = new ArrayList<SVNCommit>();
		ArrayList<SVNCommit> fixingRevs = new ArrayList<SVNCommit>();
		BugModule bugs = new BugModule();
		int totalRevs = revisions.size();
		List<SVNTicket> issues = new ArrayList<>();
		System.out.println(nullCheck.url);
		try {
			issues = bugs.getIssues(nullCheck.projName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Revisions and Issues: " + totalRevs + " " + issues.size());



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
		 * A loop for comparing all the commits with its previous commit.
		 */

		for (int i = totalRevs - 1; i > 0; i--) {
			SVNCommit revisionOld = revisions.get(i);
			SVNCommit revisionNew = revisions.get(i - 1);
			// get all the diffs of this commit from previous commit.
			ArrayList<SVNLogEntry> diffs = nullCheck.svn.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
			/*
			 * A loop for handling all the diffs fro this commit and previous
			 * commit.
			 */
			String commitMsg = revisionNew.getMessage();
			if (bugs.isFixingRevision(commitMsg)) {
				fixingRevs.add(revisionNew);
			}
			for (SVNLogEntry diff : diffs) {
				if (bugs.isFixingRevision(commitMsg, issues)) {
					// count the added null checks.
					Map<String, SVNLogEntryPath> changes = diff.getChangedPaths();
					for (String str : changes.keySet()) {
						SVNLogEntryPath changedPath = changes.get(str);
						int count = nullCheck.countNullCheckAdditions(revisionNew, revisionOld, changedPath);
						if (count > 0) {
							nullFixingRevs.add(revisionNew);
						}
					}
				}
			}
		}
		long endTime = System.currentTimeMillis();
		HashMap<String, Integer> result = new HashMap<>();
		result.put("total revs", totalRevs);
		result.put("fixing revisions", fixingRevs.size());
		result.put("Null fixing revisions", nullFixingRevs.size());
		Visualization.saveGraph(result, args[1]+ nullCheck.projName+"_nullCheck.html");
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}

	private int countNullCheckAdditions(SVNCommit lastCommitId, SVNCommit oldCommit, SVNLogEntryPath changedPath) {
		int numOfNullCheckAdds = 0;
		if (changedPath.getType() == SVNLogEntryPath.TYPE_DELETED
				|| changedPath.getType() == SVNLogEntryPath.TYPE_REPLACED
				|| changedPath.getType() == SVNLogEntryPath.TYPE_ADDED) {
			return numOfNullCheckAdds;
		} else if (changedPath.getType() == SVNLogEntryPath.TYPE_MODIFIED) {
			int nullInOld = 0;
			int nullInNew = 0;
			try {
				nullInOld = countNullChecks(oldCommit, changedPath.getPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				nullInNew = countNullChecks(lastCommitId, changedPath.getPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (nullInNew > nullInOld) {
				numOfNullCheckAdds = numOfNullCheckAdds + (nullInNew - nullInOld);
			}
		}
		return numOfNullCheckAdds;
	}

	/*
	 * Reads a file and computes the null checks.
	 */
	private int countNullChecks(SVNCommit lastCommitId, String path) throws IOException {
		String fileContent;
		try {
			fileContent = this.svn.getFileContent(path, lastCommitId.getId(), new SVNProperties(),
					new ByteArrayOutputStream());
			ASTNode ast = this.svn.createAst(fileContent);
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