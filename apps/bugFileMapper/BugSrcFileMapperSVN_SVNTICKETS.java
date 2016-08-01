package bugFileMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import svnConnector.SVNCommit;
import svnConnector.SVNConnector;
import svnConnector.SVNRepositoryCloner;
import svnConnector.SVNTicket;

/**
 * Created by nmtiwari on 7/20/16.
 * A class for mapping the files with bugs. This class lists all the
 * files along with all the bugs which were related to some change in
 * this files.
 * Note: Class does not check for what the change was but only checks if
 * it was in the same commit, which fixed the bug.
 */
public class BugSrcFileMapperSVN_SVNTICKETS {
    private SVNConnector svn;
    private String userName;
    private String projName;
    private HashMap<String, List<Integer>> fileBugIndex;

    private BugSrcFileMapperSVN_SVNTICKETS(String repoPath) {
        this.svn = new SVNConnector(repoPath);
        String[] details = repoPath.split("/");
        this.projName = details[details.length - 1];
        this.userName = details[details.length - 2];
        this.fileBugIndex = new HashMap<>();
    }

    /*
     * url must be of form: username@url
     */
    public BugSrcFileMapperSVN_SVNTICKETS(String url, String path) {
        this.userName = url.substring(0, url.indexOf('@'));
        url = url.substring(url.indexOf('@') + 1);
        this.projName = url.substring(url.lastIndexOf('/') + 1);
        try {
			SVNRepositoryCloner.clone(url, path);
		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.svn = new SVNConnector(path);
    }

    /*
* Main function for FileAssociation Mining
*/
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        int index = 0;
        BugSrcFileMapperSVN_SVNTICKETS bugsrcMapper = null;
        // path of the repository
        if (args.length < 1) {
            bugsrcMapper = new BugSrcFileMapperSVN_SVNTICKETS("/Users/nmtiwari/Desktop/test/pagal/__clonedByBoa/projects/paninij");
        } else if (args.length == 2) {
            bugsrcMapper = new BugSrcFileMapperSVN_SVNTICKETS(args[0], args[1]);
        } else {
            bugsrcMapper = new BugSrcFileMapperSVN_SVNTICKETS(args[0]);
        }
        // get all the revisions of the project
        ArrayList<SVNCommit> revisions = bugsrcMapper.svn.getAllRevisions();
        int totalRevs = revisions.size();
        //get all the issues of the projects.
        List<SVNTicket> issues = bugsrcMapper.svn.getIssues(bugsrcMapper.userName, bugsrcMapper.projName);

        SVNRepository repository = bugsrcMapper.svn.getRepository();

        // check all the revisions
        for (int i = 0; i < totalRevs; i++) {
        	System.out.println(i);
            SVNCommit revision = revisions.get(i);
            // check if the revision is bug fixing revision or a simple revision
            if(bugsrcMapper.svn.isFixingRevision(revision.getMessage(), issues)){
                // get all the files of the revisions
				List<String> files = revision.getFiles();
				for (String name : files) {
				    List<Integer> bugs = bugsrcMapper.svn.getIssueIDsFromCommitLog(revision.getMessage(), issues);
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
