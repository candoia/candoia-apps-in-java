package setting5.bugFileMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;

public class Mining {
	private VCSModule svn;
	private HashMap<String, List<Integer>> fileBugIndex;
	private String bugURL;
	private String product;

	public Mining(String url, String path, String bug_url) {
		url = url.substring(url.indexOf('@') + 1);
		try {
			ForgeModule.clone(url, path);
		} catch (SVNException e) {
			e.printStackTrace();
		}
		this.svn = new VCSModule(path);
		this.bugURL = bug_url.substring(bug_url.indexOf('@') + 1);
		this.product = bug_url.substring(0, bug_url.indexOf('@'));
	}

	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		// path of the repository
		if (args.length == 3) {
			bugsrcMapper = new Mining(args[0], args[1], args[2]);
		} else {
			bugsrcMapper = new Mining("nmtiwari@/Users/nmtiwari/Desktop/test/pagal/panini/",
					"/Users/nmtiwari/Desktop/test/pagal/panini/svn", "Tomcat 8@https://bz.apache.org/bugzilla");
		}
		ArrayList<SVNCommit> revisions = bugsrcMapper.svn.getAllRevisions();
		int totalRevs = revisions.size();
		BugModule bugs = new BugModule();
		List<SVNTicket> issues = new ArrayList<>();
		System.out.println(bugsrcMapper.bugURL + "\n" + bugsrcMapper.product);
		try {
			issues = bugs.getIssues(bugsrcMapper.bugURL, bugsrcMapper.product);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (int i = 0; i < totalRevs; i++) {
			SVNCommit revision = revisions.get(i);
			if (bugs.isFixingRevision(revision.getMessage(), issues)) {
				List<String> files = revision.getFiles();
				for (String name : files) {
					List<Integer> bugIds = bugs.getIssueIDsFromCommitLog(revision.getMessage(), issues);
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
		Visualization.saveGraph(bugCounter, args[1]  + "bugFileMapper.html");
	}
}
