package setting4.dev_activity;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Mining {
	private VCSModule git;
	public String url;

	private Mining(String url, String path) {
		this.url = url;
		url = url.substring(url.indexOf('@') + 1);
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(this.url, path);
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
		String data = "Revisoin Developer-------->Size-------->Descriptions\n";
		HashSet<String> dev = new HashSet<>();
		for (int i = (int) (totalRevs - 1); i > 0; i--) {
			RevCommit revisionOld = revisions.get(i);
			RevCommit revisionNew = revisions.get(i - 1);
			if(!dev.contains(revisionOld.getAuthorIdent().getEmailAddress())){
				dev.add(revisionOld.getAuthorIdent().getEmailAddress());
				data += revisionOld.getAuthorIdent().getEmailAddress() + "-------->";
				data += revisionOld.getAuthorIdent().getWhen().toString() + "-------->";
				try {
					List<DiffEntry> diffs = churn.git.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
					data += diffs.size() + "-------->";
				} catch (RevisionSyntaxException | IOException | GitAPIException e) {
					e.printStackTrace();
				}
				data += revisionOld.getShortMessage() + "\n";
			}
		}
		Visualization.saveGraph(data, args[1] + "_" + churn.url.substring(churn.url.lastIndexOf('/') + 1) + ".html");
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}
}
