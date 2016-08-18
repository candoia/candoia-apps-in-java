package customizations.bugsrcmapper.barToPieChart;

import br.ufpe.cin.groundhog.Issue;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nmtiwari on 7/20/16. A class for mapping the files with bugs. This
 * class lists all the files along with all the bugs which were related to some
 * change in this files. Note: Class does not check for what the change was but
 * only checks if it was in the same commit, which fixed the bug.
 */
public class Mining {
	private VCSModule git;
	private String userName;
	private String projName;
	private HashMap<String, List<Integer>> fileBugIndex;

	public Mining(String url, String path) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(url, path);
			} catch (IOException | GitAPIException e) {
				e.printStackTrace();
			}

		}
		this.git = new VCSModule(path);
		this.fileBugIndex = new HashMap<>();
	}

	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		if (args.length == 2) {
			bugsrcMapper = new Mining(args[0], args[1]);
		}
		ArrayList<RevCommit> revisions = bugsrcMapper.git.getAllRevisions();
		int totalRevs = revisions.size();
		BugModule bugIds = new BugModule(bugsrcMapper.userName, bugsrcMapper.projName);
		List<Issue> issues = bugIds.getIssues();
		Repository repository = bugsrcMapper.git.getRepository();
		for (int i = 0; i < totalRevs; i++) {
			RevCommit revision = revisions.get(i);
			if (bugIds.isFixingRevision(revision.getFullMessage(), issues)) {
				try {
					List<String> files = bugsrcMapper.git.readElementsAt(repository, revision.getId().getName());
					for (String name : files) {
						List<Integer> bugs = bugIds.getIssueIDsFromCommitLog(revision.getFullMessage(), issues);
						if (!bugsrcMapper.fileBugIndex.containsValue(name)) {
							bugsrcMapper.fileBugIndex.put(name, bugs);
						} else {
							List<Integer> alreadyAssigned = bugsrcMapper.fileBugIndex.get(name);
							for (Integer bugId : bugs) {
								if (!alreadyAssigned.contains(bugId)) {
									alreadyAssigned.add(bugId);
								}
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		HashMap<String, Integer> bugCounter = new HashMap<>();
		for (String name : bugsrcMapper.fileBugIndex.keySet()) {
			int count = bugsrcMapper.fileBugIndex.get(name).size();
			System.out.println(name + " -> " + count);
			if (count > 2) {
				bugCounter.put(name, count);
			}
		}
		Visualization.saveGraph(bugCounter, args[1] + "/bugSrcMapper_" + bugsrcMapper.projName + ".html");
	}
}
