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
 */
public class BugSrcFileMapper {
    private GitConnector git;
    private String userName;
    private String projName;
    private HashMap<String, List<Integer>> fileBugIndex;

    private BugSrcFileMapper(String repoPath) {
        this.git = new GitConnector(repoPath);
        String[] details = repoPath.split("/");
        this.projName = details[details.length - 1];
        this.userName = details[details.length - 2];
        this.fileBugIndex = new HashMap<>();
    }

    /*
     * url must be of form: username@url
     */
    public BugSrcFileMapper(String url, String path) {
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
        BugSrcFileMapper bugsrcMapper = null;
        // path of the repository
        if (args.length < 1) {
            bugsrcMapper = new BugSrcFileMapper("/Users/nmtiwari/Desktop/test/pagal/__clonedByBoa/nmtiwari/compiler");
        } else if (args.length == 2) {
            bugsrcMapper = new BugSrcFileMapper(args[1], args[0]);
        } else {
            bugsrcMapper = new BugSrcFileMapper(args[0]);
        }

        ArrayList<RevCommit> revisions = bugsrcMapper.git.getAllRevisions();
        int totalRevs = revisions.size();
        List<Issue> issues = bugsrcMapper.git.getIssues(bugsrcMapper.userName, bugsrcMapper.projName);

        Repository repository = bugsrcMapper.git.getRepository();
        for (int i = 0; i < totalRevs; i++) {
            RevCommit revision = revisions.get(i);
            if(bugsrcMapper.git.isFixingRevision(revision.getFullMessage(), issues)){
                try {
                    List<String> files = bugsrcMapper.git.readElementsAt(repository, revision.getId().getName());
                    for (String name : files) {
                        List<Integer> bugs = bugsrcMapper.git.getIssueIDsFromCommitLog(revision.getFullMessage());
                        if (!bugsrcMapper.fileBugIndex.containsValue(name)) {
                            bugsrcMapper.fileBugIndex.put(name,bugs);
                        }else{
                            List<Integer> alreadyAssigned = bugsrcMapper.fileBugIndex.get(name);
                            for(Integer bugId: bugs){
                                if(!alreadyAssigned.contains(bugId)){
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
        System.out.println("Total buggy files: " + bugsrcMapper.fileBugIndex.size());
//        for(String name: bugsrcMapper.fileBugIndex.keySet()){
//            System.out.println(name + " -> " + bugsrcMapper.fileBugIndex.get(name));
//        }
    }
}
