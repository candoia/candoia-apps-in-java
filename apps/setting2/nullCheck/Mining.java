package setting2.nullCheck;

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
import org.tmatesoft.svn.core.SVNException;
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
	private String bugURL;
	private String product;

	private Mining(String repoPath, String bug_url) {
		this.svn = new VCSModule(repoPath);
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
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(url, path);
			} catch (SVNException e) {
				e.printStackTrace();
			}
		}
		this.svn = new VCSModule(path);
		this.bugURL = bug_url.substring(bug_url.indexOf('@') + 1);
		this.product = bug_url.substring(0, bug_url.indexOf('@'));
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		Mining nullCheck = null;
		if (args.length == 3) {
			nullCheck = new Mining(args[0], args[1], args[2]);
		} else {
			nullCheck = new Mining("/Users/nmtiwari/Desktop/test/pagal/paninij",
					"Tomcat 8@https://bz.apache.org/bugzilla");
		}

		ArrayList<SVNCommit> revisions = nullCheck.svn.getAllRevisions();
		ArrayList<SVNCommit> nullFixingRevs = new ArrayList<SVNCommit>();
		ArrayList<SVNCommit> fixingRevs = new ArrayList<SVNCommit>();
		BugModule bugs = new BugModule();
		int totalRevs = revisions.size();
		List<b4j.core.Issue> issues = new ArrayList<>();
		System.out.println(nullCheck.bugURL + "\n" + nullCheck.product);
		try {
			issues = bugs.importBugs(nullCheck.bugURL, nullCheck.product);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Revisions and Issues: " + totalRevs + " " + issues.size());

		for (int i = totalRevs - 1; i > 0; i--) {
			SVNCommit revisionOld = revisions.get(i);
			SVNCommit revisionNew = revisions.get(i - 1);
			ArrayList<SVNLogEntry> diffs = nullCheck.svn.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
			String commitMsg = revisionNew.getMessage();
			if (bugs.isFixingRevision(commitMsg)) {
				fixingRevs.add(revisionNew);
			}
			for (SVNLogEntry diff : diffs) {
				if (bugs.isFixingRevision(commitMsg, issues)) {
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
		Visualization.saveGraph(result, "/Users/nmtiwari/Desktop/null.html");
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