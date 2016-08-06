package settging1.churnRate;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Created by nmtiwari on 7/24/16.
 */
public class Mining {
    private VCSModule git;
    private String userName;
    private String projName;
    private String localPath;

    /*
     * url must be of form: username@url
     */
    private Mining(String url, String path) {
        this.userName = url.substring(0, url.indexOf('@'));
        url = url.substring(url.indexOf('@') + 1);
        this.projName = url.substring(url.lastIndexOf('/') + 1);
        VCSModule.cloneRepo(url, path);
        this.git = new VCSModule(path);
    }

    /*
     * Main function for churn rate
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Mining churn = null;
        // path of the repository
       if (args.length == 2) {
            churn = new Mining(args[0], args[1]);
        } else {
            throw new IllegalArgumentException();
        }

        ArrayList<RevCommit> revisions = churn.git.getAllRevisions();
        ArrayList<RevCommit> nullFixingRevs = new ArrayList<RevCommit>();
        ArrayList<RevCommit> fixingRevs = new ArrayList<RevCommit>();
        int totalRevs = revisions.size();

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
        HashMap<String, Integer> churnDetails = new HashMap<>();
        for (int i = totalRevs - 1; i > 0; i--) {
            RevCommit revisionOld = revisions.get(i);
            RevCommit revisionNew = revisions.get(i - 1);
            try {
                // get all the diffs of this commit from previous commit.
                List<DiffEntry> diffs = churn.git.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
                for (DiffEntry diff : diffs) {
                    churnDetails = churn.countNumberOfLinesChange(revisionNew.getId(), revisionOld.getId(), diff, churnDetails);
                }
            } catch (RevisionSyntaxException | IOException | GitAPIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        HashMap<String, Double> result = new HashMap<>();
        for (String key : churnDetails.keySet()) {
            double count = churnDetails.get(key) / totalRevs;
            if (count > 3)
                result.put(key, count);
        }
        Visualization.saveGraph(result, "/Users/nmtiwari/Desktop/Churn.html");
        System.out.println("Time: " + (endTime - startTime) / 1000.000);
    }

    /*
    * Counts number of lines in file
    */
    public static int countLines(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            is.close();
        }
        return 0;
    }

    /*
     * A function to check all the number of lines changed in the file
     */
    private HashMap<String, Integer> countNumberOfLinesChange(ObjectId lastCommitId, ObjectId oldCommit, DiffEntry diff, HashMap<String, Integer> churnDetail) {
        int numOfNullCheckAdds = 0;
        String oldPath = diff.getOldPath();
        String newPath = diff.getNewPath();
        int changes = 0;
        try {
            if (diff.getChangeType() == DiffEntry.ChangeType.DELETE) {
                changes = countLines(this.localPath + "/" + oldPath);
                fillDetail(oldPath, changes, churnDetail);
            } else if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
                changes = countLines(this.localPath + "/" + newPath);
                fillDetail(newPath, changes, churnDetail);
            } else if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY) {
                int difference = countLines(this.localPath + "/" + oldPath) - countLines(this.localPath + "/" + newPath);
                changes = difference > 0 ? difference : -1 * difference;
                fillDetail(oldPath, changes, churnDetail);
            } else if (diff.getChangeType() == DiffEntry.ChangeType.RENAME) {
                changes = countLines(this.localPath + "/" + newPath);
                fillDetail(newPath, changes, churnDetail);
            }
        } catch (IOException e) {
            return churnDetail;
        }
        return churnDetail;
    }

    /*
     * A function for adding data in map
     */
    private HashMap<String, Integer> fillDetail(String key, int value, HashMap<String, Integer> map) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + value);
        } else {
            map.put(key, value);
        }
        return map;
    }

}
