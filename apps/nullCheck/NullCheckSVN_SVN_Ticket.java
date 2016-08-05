package nullCheck;

import java.io.ByteArrayOutputStream;
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
import org.osgi.service.log.LogEntry;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNProperties;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import br.ufpe.cin.groundhog.Issue;
import svnConnector.SVNCommit;
import svnConnector.SVNConnector;
import svnConnector.SVNTicket;

/**
 * Created by nmtiwari on 7/9/16. A class to check all the Null Checks in the
 * subjected repository.
 */
public class NullCheckSVN_SVN_Ticket {
	private String userName;
	private String projName;
	private SVNConnector svn;

	private NullCheckSVN_SVN_Ticket(String repoPath) {
		this.svn = new SVNConnector(repoPath);
		String[] details = repoPath.split("/");
		this.projName = details[details.length - 1];
		this.userName = details[details.length - 2];
	}

	/*
	 * url must be of form: username@url
	 */
	private NullCheckSVN_SVN_Ticket(String url, String path) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		SVNConnector.cloneRepo(url, path);
		this.svn = new SVNConnector(path);
	}

	/*
	 * Main function for NullCheckGit_GIT_Ticket
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		NullCheckSVN_SVN_Ticket nullCheck = null;
		// path of the repository
		if (args.length < 1) {
			nullCheck = new NullCheckSVN_SVN_Ticket("/Users/nmtiwari/Desktop/test/pagal/paninij");
		} else if (args.length == 2) {
			nullCheck = new NullCheckSVN_SVN_Ticket(args[1], args[0]);
		} else {
			nullCheck = new NullCheckSVN_SVN_Ticket(args[0]);
		}

		ArrayList<SVNCommit> revisions = nullCheck.svn.getAllRevisions();
		ArrayList<SVNCommit> nullFixingRevs = new ArrayList<SVNCommit>();
		ArrayList<SVNCommit> fixingRevs = new ArrayList<SVNCommit>();
		List<SVNTicket> issues = nullCheck.svn.getIssues(nullCheck.userName, nullCheck.projName);
		int totalRevs = revisions.size();
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
			if (nullCheck.svn.isFixingRevision(commitMsg)) {
				fixingRevs.add(revisionNew);
			}
			for (SVNLogEntry diff : diffs) {
				if (nullCheck.svn.isFixingRevision(commitMsg, issues)) {
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
		NullFixingGraph.saveGraph(result, "/Users/nmtiwari/Desktop/null.html");
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