package nullCheck;

import br.ufpe.cin.groundhog.Issue;
import gitConnector.GitConnector;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nmtiwari on 7/9/16.
 * A class to check all the Null Checks in the subjected repository.
 */
public class NullCheckGit_GIT_Ticket {
    private GitConnector git;
    private String userName;
    private String projName;

    private NullCheckGit_GIT_Ticket(String repoPath) {
        this.git = new GitConnector(repoPath);
        String[] details = repoPath.split("/");
        this.projName = details[details.length - 1];
        this.userName = details[details.length - 2];
    }

    /*
     * url must be of form: username@url
     */
    private NullCheckGit_GIT_Ticket(String url, String path) {
        this.userName = url.substring(0, url.indexOf('@'));
        url = url.substring(url.indexOf('@') + 1);
        this.projName = url.substring(url.lastIndexOf('/') + 1);
        GitConnector.cloneRepo(url, path);
        this.git = new GitConnector(path);
    }

    /*
     * Main function for NullCheckGit_GIT_Ticket
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        NullCheckGit_GIT_Ticket nullCheck = null;
        // path of the repository
        if (args.length < 1) {
            nullCheck = new NullCheckGit_GIT_Ticket("/Users/nmtiwari/Desktop/test/pagal/__clonedByBoa/boalang/compiler");
        } else if (args.length == 2) {
            nullCheck = new NullCheckGit_GIT_Ticket(args[1], args[0]);
        } else {
            nullCheck = new NullCheckGit_GIT_Ticket(args[0]);
        }

        ArrayList<RevCommit> revisions = nullCheck.git.getAllRevisions();
        ArrayList<RevCommit> nullFixingRevs = new ArrayList<RevCommit>();
        ArrayList<RevCommit> fixingRevs = new ArrayList<RevCommit>();
        List<Issue> issues = nullCheck.git.getIssues(nullCheck.userName, nullCheck.projName);
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
            RevCommit revisionOld = revisions.get(i);
            RevCommit revisionNew = revisions.get(i - 1);
            try {
                // get all the diffs of this commit from previous commit.
                List<DiffEntry> diffs = nullCheck.git.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
				/*
				 * A loop for handling all the diffs fro this commit and
				 * previous commit.
				 */
                String commitMsg = revisionNew.getFullMessage();
                if (nullCheck.git.isFixingRevision(commitMsg)) {
                	fixingRevs.add(revisionNew);
                } 
                for (DiffEntry diff : diffs) {
                    	if(nullCheck.git.isFixingRevision(commitMsg, issues)){
                          // count the added null checks.
                            int count = nullCheck.countNullCheckAdditions(revisionNew.getId(), revisionOld.getId(), diff);
                            if (count > 0) {
                                nullFixingRevs.add(revisionNew);
                          }
                    	}
                }
            } catch (RevisionSyntaxException | IOException | GitAPIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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


    private int countNullCheckAdditions(ObjectId lastCommitId, ObjectId oldCommit, DiffEntry diff) {
        int numOfNullCheckAdds = 0;
        String oldPath = diff.getOldPath();
        String newPath = diff.getNewPath();
        int nullInOld = 0;
        int nullInNew = 0;
        if (diff.getChangeType() == DiffEntry.ChangeType.DELETE || diff.getChangeType() == DiffEntry.ChangeType.RENAME
                || diff.getChangeType() == DiffEntry.ChangeType.ADD || diff.getChangeType() == DiffEntry.ChangeType.COPY) {
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
//                numOfNullCheckAdds++;
                numOfNullCheckAdds = numOfNullCheckAdds + (nullInNew - nullInOld);
            }
        }
        return numOfNullCheckAdds;
    }


    /*
    * Reads a file and computes the null checks.
    */
    private int countNullChecks(ObjectId lastCommitId, String path)
            throws IOException {
        String fileContent;
        try {
            fileContent = this.git.readFile(lastCommitId, path);
            ASTNode ast = this.git.createAst(fileContent);
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
                    if (node.getOperator() == InfixExpression.Operator.EQUALS || node.getOperator() == InfixExpression.Operator.NOT_EQUALS) {
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