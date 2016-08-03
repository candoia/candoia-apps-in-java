package churnRate;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNProperties;

import gitConnector.GitConnector;
import svnConnector.SVNCommit;
import svnConnector.SVNConnector;

/**
 * Created by nmtiwari on 7/24/16.
 */
public class ChurnRateSVN {
	private SVNConnector svn;
	private String userName;
	private String projName;
	private String localPath;

	private ChurnRateSVN(String repoPath) {
		this.svn = new SVNConnector(repoPath);
		String[] details = repoPath.split("/");
		this.projName = details[details.length - 1];
		this.userName = details[details.length - 2];
		this.localPath = repoPath.substring(0, repoPath.lastIndexOf('/'));
	}

	/*
	 * url must be of form: username@url
	 */
	private ChurnRateSVN(String url, String path) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		GitConnector.cloneRepo(url, path);
		this.svn = new SVNConnector(path);
	}

	/*
	 * Main function for churn rate
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		ChurnRateSVN churn = null;
		// path of the repository
		if (args.length < 1) {
			churn = new ChurnRateSVN("/Users/nmtiwari/Desktop/test/pagal/projects");
		} else if (args.length == 2) {
			churn = new ChurnRateSVN(args[1], args[0]);
		} else {
			churn = new ChurnRateSVN(args[0]);
		}

		ArrayList<SVNCommit> revisions = churn.svn.getAllRevisions();
		ArrayList<SVNCommit> nullFixingRevs = new ArrayList<SVNCommit>();
		ArrayList<SVNCommit> fixingRevs = new ArrayList<SVNCommit>();
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
			SVNCommit revisionOld = revisions.get(i);
			SVNCommit revisionNew = revisions.get(i - 1);
			try {
				// get all the diffs of this commit from previous commit.
				Collection<?> diffs = churn.svn.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
				Iterator<?> iter = diffs.iterator();
				while (iter.hasNext()) {
					SVNLogEntry entry = (SVNLogEntry) iter.next();
					Map<String, SVNLogEntryPath> changedPaths = entry.getChangedPaths();
					for (String str : changedPaths.keySet()) {
						SVNLogEntryPath path = changedPaths.get(str);
						churnDetails = churn.countNumberOfLinesChange(path, revisionNew.getId(), revisionOld.getId(),
								entry, churnDetails);
					}

				}
			} catch (RevisionSyntaxException e) {
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
		ChurnRateGraph.saveGraph(result, "/Users/nmtiwari/Desktop/Churn.html");
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}

	/*
	 * Counts number of lines in file
	 */
	public static int countLines(String content) {
		String[] lines = content.split("\r\n|\r|\n");
		return lines.length;
	}

	/*
	 * A function to check all the number of lines changed in the file
	 */
	private HashMap<String, Integer> countNumberOfLinesChange(SVNLogEntryPath file, long newId, long oldId,
			SVNLogEntry entry, HashMap<String, Integer> churnDetail) {
		int numOfNullCheckAdds = 0;
		int changes = 0;
		String filePath = file.getPath();
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		if (file.getType() == SVNLogEntryPath.TYPE_DELETED) {
			String content = this.svn.getFileContent(filePath, oldId, new SVNProperties(), os);
			changes = countLines(content);
			fillDetail(filePath, changes, churnDetail);
		} else if (file.getType() == SVNLogEntryPath.TYPE_ADDED) {
			String newContent = this.svn.getFileContent(filePath, newId, new SVNProperties(), os);
			changes = countLines(newContent);
			fillDetail(filePath, changes, churnDetail);
		} else if (file.getType() == SVNLogEntryPath.TYPE_MODIFIED) {
			String oldContent = this.svn.getFileContent(filePath, oldId, new SVNProperties(), os);
			String newContent = this.svn.getFileContent(filePath, newId, new SVNProperties(),
					new ByteArrayOutputStream());
			int difference = countLines(oldContent) - countLines(newContent);
			changes = difference > 0 ? difference : -1 * difference;
			fillDetail(filePath, changes, churnDetail);
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
