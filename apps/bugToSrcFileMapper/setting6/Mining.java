package bugToSrcFileMapper.setting6;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

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
	private HashMap<String, List<Integer>> fileBugIndex;
	private String bugURL;
	private String product;

	/*
	 * url must be of form: username@url
	 */
	public Mining(String url, String path, String bug_url) {
		url = url.substring(url.indexOf('@') + 1);
		try {
			ForgeModule.clone(url, path);
		} catch (IOException | GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.git = new VCSModule(path);
		this.bugURL = bug_url.substring(bug_url.indexOf('@') + 1);
		this.product = bug_url.substring(0, bug_url.indexOf('@'));
		fileBugIndex = new HashMap<>();
	}

	/*
	 * Main function for FileAssociation Mining
	 */
	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		// path of the repository
		if (args.length == 3) {
			bugsrcMapper = new Mining(args[0], args[1], args[2]);
		} else {
			bugsrcMapper = new Mining("nmtiwari@https://github.com/qos-ch/slf4j",
					"/Users/nmtiwari/Desktop/test/pagal/slf4j", "SLF4J@http://jira.qos.ch/");
		}
		// get all the revisions of the project
		ArrayList<RevCommit> revisions = bugsrcMapper.git.getAllRevisions();
		int totalRevs = revisions.size();
		// get all the issues of the projects.
		BugModule bugs = new BugModule(bugsrcMapper.bugURL, bugsrcMapper.product);
		List<br.ufpe.cin.groundhog.Issue> issues = new ArrayList<>();
		try {
			issues = bugs.getIssues();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Repository repository = bugsrcMapper.git.getRepository();

		// check all the revisions
		for (int i = 0; i < totalRevs; i++) {
			RevCommit revision = revisions.get(i);
			// check if the revision is bug fixing revision or a simple revision
			if (bugs.isFixingRevision(revision.getFullMessage(), issues)) {
				try {
					// get all the files of the revisions
					List<String> files = bugsrcMapper.git.readElementsAt(repository, revision.getId().getName());
					// check all the files if they have not been recorded for
					// this bugs in this commit then record
					// else you simply ignore this
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
