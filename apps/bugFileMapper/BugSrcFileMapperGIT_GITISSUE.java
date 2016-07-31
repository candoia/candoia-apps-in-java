package bugFileMapper;

import br.ufpe.cin.groundhog.Issue;
import gitConnector.GitConnector;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nmtiwari on 7/20/16.
 * A class for mapping the files with bugs. This class lists all the
 * files along with all the bugs which were related to some change in
 * this files.
 * Note: Class does not check for what the change was but only checks if
 * it was in the same commit, which fixed the bug.
 */
public class BugSrcFileMapperGIT_GITISSUE {
    private GitConnector git;
    private String userName;
    private String projName;
    private HashMap<String, List<Integer>> fileBugIndex;

    private BugSrcFileMapperGIT_GITISSUE(String repoPath) {
        this.git = new GitConnector(repoPath);
        String[] details = repoPath.split("/");
        this.projName = details[details.length - 1];
        this.userName = details[details.length - 2];
        this.fileBugIndex = new HashMap<>();
    }

    /*
     * url must be of form: username@url
     */
    public BugSrcFileMapperGIT_GITISSUE(String url, String path) {
        this.userName = url.substring(0, url.indexOf('@'));
        url = url.substring(url.indexOf('@') + 1);
        this.projName = url.substring(url.lastIndexOf('/') + 1);
        GitConnector.cloneRepo(url, path);
        this.git = new GitConnector(path);
    }

    /*
* Main function for FileAssociation Mining
*/
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        int index = 0;
        BugSrcFileMapperGIT_GITISSUE bugsrcMapper = null;
        // path of the repository
        if (args.length < 1) {
            bugsrcMapper = new BugSrcFileMapperGIT_GITISSUE("/Users/nmtiwari/Desktop/test/pagal/__clonedByBoa/boalang/compiler");
        } else if (args.length == 2) {
            bugsrcMapper = new BugSrcFileMapperGIT_GITISSUE(args[1], args[0]);
        } else {
            bugsrcMapper = new BugSrcFileMapperGIT_GITISSUE(args[0]);
        }
        // get all the revisions of the project
        ArrayList<RevCommit> revisions = bugsrcMapper.git.getAllRevisions();
        int totalRevs = revisions.size();
        //get all the issues of the projects.
        List<Issue> issues = bugsrcMapper.git.getIssues(bugsrcMapper.userName, bugsrcMapper.projName);

        // get the git repository
        Repository repository = bugsrcMapper.git.getRepository();

        // checl all the revisions
        for (int i = 0; i < totalRevs; i++) {
            RevCommit revision = revisions.get(i);
            // check if the revision is bug fixing revision or a simple revision
            if (bugsrcMapper.git.isFixingRevision(revision.getFullMessage(), issues)) {
                try {
                    // get all the files of the revisions
                    List<String> files = bugsrcMapper.git.readElementsAt(repository, revision.getId().getName());
                    // check all the files if they have not been recorded for this bugs in this commit then  record
                    // else you simply ignore this
                    for (String name : files) {
                        List<Integer> bugs = bugsrcMapper.git.getIssueIDsFromCommitLog(revision.getFullMessage(), issues);
                        if (!bugsrcMapper.fileBugIndex.containsValue(name)) {
                            bugsrcMapper.fileBugIndex.put(name, bugs);
                        } else {
                            List<Integer> alreadyAssigned = bugsrcMapper.fileBugIndex.get(name);
                            for (Integer bugId : bugs) {
                                if (!alreadyAssigned.contains(bugId)) {
                                    alreadyAssigned.add(bugId);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        // print all the values
        System.out.println(issues.toString());
        HashMap<String, Integer> bugCounter = new HashMap<>();
        System.out.println("Total buggy files: " + bugsrcMapper.fileBugIndex.size());
        for (String name : bugsrcMapper.fileBugIndex.keySet()) {
            int count = bugsrcMapper.fileBugIndex.get(name).size();
            System.out.println(name + " -> " + count);
            if (count > 1) {
                bugCounter.put(name, count);
            }
        }
        BugSrcFileMapperGraph.saveGraph(bugCounter, "/Users/nmtiwari/Desktop/bug.html");
    }
}