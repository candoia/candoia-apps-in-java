package setting1.module_dev_mapper;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.revwalk.RevCommit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
		HashMap<String, ArrayList<String>> churnDetails = new HashMap<>();
		for (int i = (int) (totalRevs - 1); i > 0; i--) {
			RevCommit revisionOld = revisions.get(i);
			RevCommit revisionNew = revisions.get(i - 1);
			try {
				List<DiffEntry> diffs = churn.git.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
				for (DiffEntry diff : diffs) {
					String path = diff.getOldPath() == null ? diff.getNewPath():diff.getOldPath();
					if(path.contains("/")){
						path = path.substring(0, path.lastIndexOf('/'));
					}
					if (churnDetails.containsKey(path)) {
						ArrayList<String> devs = churnDetails.get(path);
						if (!devs.contains(revisionOld.getAuthorIdent().getName())){
							churnDetails.get(path).add(revisionOld.getAuthorIdent().getName());
						}
					}
					else{
						ArrayList<String> devs = new ArrayList<>();
						  devs.add(revisionOld.getAuthorIdent().getName());
						churnDetails.put(path, devs);
					}
				}
			} catch (RevisionSyntaxException | IOException | GitAPIException e) {
				e.printStackTrace();
			}
		}
		String data = "";
		HashMap<String, Double> result = new HashMap<>();
		for (String key : churnDetails.keySet()) {
			data += key + "--->";
			ArrayList<String> devs = churnDetails.get(key);
			for(String dev: devs){
				data += dev + ", ";
			}
			data += "\n";
		}
		Visualization.saveGraph(data, args[1] + "_" + churn.url.substring(churn.url.lastIndexOf('/') + 1) + ".html");
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) / 1000.000);
	}
}
