package setting2.churnRate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;

import settging1.churnRate.Visualization;

/**
 * Created by nmtiwari on 7/24/16.
 */
public class Mining {
	private VCSModule svn;
	public String url;
	private Mining(String repoPath) {
		this.svn = new VCSModule(repoPath);
	}

	/*
	 * url must be of form: username@url
	 */
	private Mining(String url, String path) {
		this.url = url;
		url = url.substring(url.indexOf('@') + 1);
		try {
			ForgeModule.clone(url, path);
		} catch (SVNException e) {
			e.printStackTrace();
		}
		this.svn = new VCSModule(path);
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

		ArrayList<SVNCommit> revisions = churn.svn.getAllRevisions();
		double totalRevs = revisions.size();

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
		for (int i = (int) (totalRevs - 1); i > 0; i--) {
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

						if (path.getType() == SVNLogEntryPath.TYPE_REPLACED) {
							churnDetails.put(path.getCopyPath(), churnDetails.get(path.getPath() + 1));
						} else if (path.getType() == SVNLogEntryPath.TYPE_ADDED) {
							churnDetails.put(path.getPath(), 1);
						} else if (path.getType() == SVNLogEntryPath.TYPE_MODIFIED) {
							churnDetails.put(path.getPath(), churnDetails.get(path.getPath() + 1));
						}
					}

				}
			} catch (RevisionSyntaxException e) {
				e.printStackTrace();
			}
		}
		long endTime = System.currentTimeMillis();
		HashMap<String, Double> result = new HashMap<>();
		for (String key : churnDetails.keySet()) {
			double count = churnDetails.get(key) / totalRevs;
			if (count > 0.003)
				result.put(key, count);
		}
		Visualization.saveGraph(result, args[1] + "_" + churn.url.substring(churn.url.lastIndexOf('/') + 1) + ".html");
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}

}
