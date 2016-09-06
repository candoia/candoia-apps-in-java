package setting6.top_performers;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
		HashMap<String, Integer> perf = new HashMap<>();
		Mining churn = null;
		if (args.length == 2) {
			churn = new Mining(args[0], args[1]);
		} else {
			throw new IllegalArgumentException();
		}
		ArrayList<RevCommit> revisions = churn.git.getAllRevisions();
		double totalRevs = revisions.size();
		String data = "Revisoin Time-------->Size-------->Descriptions\n";
		for(RevCommit rev: revisions){
			String author = rev.getAuthorIdent().getName();
			if(!perf.containsKey(author)){
				perf.put(author, 1);
			}else{
				perf.put(author, perf.get(author)+1);
			}
		}
		Visualization.saveGraph(perf, args[1] + "_" + churn.url.substring(churn.url.lastIndexOf('/') + 1) + ".html");
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}
}
