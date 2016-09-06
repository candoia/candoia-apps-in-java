package setting2.rev_checker;

import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;

import java.io.File;
import java.util.*;

public class Mining {
	private VCSModule svn;
	public String url;

	private Mining(String repoPath) {
		this.svn = new VCSModule(repoPath);
	}

	private Mining(String url, String path) {
		this.url = url;
		url = url.substring(url.indexOf('@') + 1);
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(this.url, path);
			} catch (SVNException e) {
				e.printStackTrace();
			}
		}

		this.svn = new VCSModule(path);
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		Mining churn = null;
		// path of the repository
		if (args.length == 2) {
			churn = new Mining(args[0], args[1]);
		} else {
			throw new IllegalArgumentException();
		}

		ArrayList<SVNCommit> revisions = churn.svn.getAllRevisions();
		double totalRevs = revisions.size();
		HashMap<String, Integer> churnDetails = new HashMap<>();
		String data = "Revisoin Time-------->Size-------->Descriptions\n";
		for (int i = (int) (totalRevs - 1); i > 0; i--) {
			SVNCommit revisionOld = revisions.get(i);
			SVNCommit revisionNew = revisions.get(i - 1);
			data += revisionOld.date + "-------->";
			try {
				// get all the diffs of this commit from previous commit.
				Collection<?> diffs = churn.svn.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
				data += diffs.size() + "-------->";
			} catch (RevisionSyntaxException e) {
				e.printStackTrace();
			}
			data += revisionOld.getMessage() + "\n";
		}
		long endTime = System.currentTimeMillis();
		HashMap<String, Double> result = new HashMap<>();
		for (String key : churnDetails.keySet()) {
			double count = churnDetails.get(key);
			if (!key.contains(".") && count > 10)
				result.put(key, count);
		}
		Visualization.saveGraph(data, args[1] + "_" + churn.url.substring(churn.url.lastIndexOf('/') + 1) + ".html");
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}

}
