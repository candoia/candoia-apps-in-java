package setting3.bugFileMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import setting3.bugFileMapper.BugModule;;
public class Mining {
	private VCSModule git;
	private HashMap<String, List<Integer>> fileBugIndex;
	private String bugURL;
	private String product;

	public Mining(String url, String path, String bug_url) {
		url = url.substring(url.indexOf('@') + 1);
		try {
			ForgeModule.clone(url, path);
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		}
		this.git = new VCSModule(path);
		this.bugURL = bug_url.substring(bug_url.indexOf('@') + 1);
		this.product = bug_url.substring(0, bug_url.indexOf('@'));
		fileBugIndex = new HashMap<>();
	}

	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		if (args.length == 3) {
			bugsrcMapper = new Mining(args[0], args[1], args[2]);
		} else {
			bugsrcMapper = new Mining("nmtiwari@https://github.com/qos-ch/slf4j",
					"/Users/nmtiwari/Desktop/test/pagal/slf4j", "SLF4J@http://jira.qos.ch/");
		}
		ArrayList<RevCommit> revisions = bugsrcMapper.git.getAllRevisions();
		int totalRevs = revisions.size();
		BugModule bugs = new BugModule(bugsrcMapper.bugURL, bugsrcMapper.product);
		List<b4j.core.Issue> issues = new ArrayList<>();
		try {
			issues = bugs.getIssues();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Repository repository = bugsrcMapper.git.getRepository();

		for (int i = 0; i < totalRevs; i++) {
			RevCommit revision = revisions.get(i);
			if (bugs.isFixingRevision(revision.getFullMessage(), issues)) {
				try {
					List<String> files = bugsrcMapper.git.readElementsAt(repository, revision.getId().getName());
					for (String name : files) {
						List<Integer> bugIds = bugs.getIssueIDsFromCommitLog(revision.getFullMessage(), issues);
						if (!bugsrcMapper.fileBugIndex.containsValue(name)) {
							bugsrcMapper.fileBugIndex.put(name, bugIds);
						} else {
							List<Integer> alreadyAssigned = bugsrcMapper.fileBugIndex.get(name);
							for (Integer bugId : bugIds) {
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
			if (count > 1) {
				bugCounter.put(name, count);
			}
		}
		Visualization.saveGraph(bugCounter, "/Users/nmtiwari/Desktop/bug.html");
	}
}
