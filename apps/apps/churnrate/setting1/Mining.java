package churnrate.setting1;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nmtiwari on 7/24/16.
 */
public class Mining {
	private VCSModule git;
	public String url;

	private Mining(String url, String path) {
		this.url = url;
		url = url.substring(url.indexOf('@') + 1);
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(url, path);
			} catch (IOException | GitAPIException e) {
				e.printStackTrace();
			}
		}
		this.git = new VCSModule(path);
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		Mining churn = null;
		if (args.length == 2) {
			churn = new Mining(args[0], args[1]);
		} else {
			throw new IllegalArgumentException();
		}

		ArrayList<RevCommit> revisions = churn.git.getAllRevisions();
		double totalRevs = revisions.size();
		HashMap<String, Integer> churnDetails = new HashMap<>();
		for (int i = (int) (totalRevs - 1); i > 0; i--) {
			RevCommit revisionOld = revisions.get(i);
			RevCommit revisionNew = revisions.get(i - 1);
			try {
				List<DiffEntry> diffs = churn.git.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
				for (DiffEntry diff : diffs) {
					if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
						churnDetails.put(diff.getNewPath(), 1);
					} else if (diff.getChangeType() == DiffEntry.ChangeType.RENAME) {
						churnDetails.put(diff.getNewPath(), churnDetails.get(diff.getOldPath() + 1));
					} else {
						String oldPath = diff.getOldPath();
						if (churnDetails.containsKey(oldPath))
							churnDetails.put(diff.getOldPath(), churnDetails.get(oldPath) + 1);
						else
							churnDetails.put(diff.getOldPath(), 1);
					}

				}
			} catch (RevisionSyntaxException | IOException | GitAPIException e) {
				e.printStackTrace();
			}
		}

		long endTime = System.currentTimeMillis();
		HashMap<String, Double> result = new HashMap<>();
		for (String key : churnDetails.keySet()) {
			double count = churnDetails.get(key) / totalRevs;
			if (count > 0.03)
				result.put(key, count);
		}
		Visualization.saveGraph(result, args[1] + "_" + churn.url.substring(churn.url.lastIndexOf('/') + 1) + ".html");
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}

}
