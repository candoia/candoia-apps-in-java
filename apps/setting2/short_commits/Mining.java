package setting2.short_commits;

import org.tmatesoft.svn.core.SVNException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

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
		HashMap<String, Integer> perf = new HashMap<>();
		for(SVNCommit com: revisions){
			int counter = com.getMessage().split(" ").length;
			if(counter <= 2){
				if(!perf.containsKey(com.committer)){
					perf.put(com.committer, 1);
				}else{
					perf.put(com.committer, perf.get(com.committer)+1);
				}
			}
		}
		Visualization.saveGraph(perf, args[1] + "_" + churn.url.substring(churn.url.lastIndexOf('/') + 1) + ".html");
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}

}
