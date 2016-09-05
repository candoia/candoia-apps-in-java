package setting2.wont_fix_bugs;

import org.tmatesoft.svn.core.SVNException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Mining {
	private String userName;
	private String projName;
	private HashMap<String, List<Integer>> fileBugIndex;
	private String bugURL;
	private String product;

	public Mining(String url, String path, String bug_url) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
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
		BugModule bugs = new BugModule();
		List<b4j.core.Issue> issues = new ArrayList<>();
		System.out.println(bugsrcMapper.bugURL + "\n" + bugsrcMapper.product);
		try {
			issues = bugs.getIssues(bugsrcMapper.bugURL, bugsrcMapper.product);
		} catch (Exception e) {
			e.printStackTrace();
		}
		HashMap<String, Integer> bugCounter = new HashMap<>();
		for (b4j.core.Issue issue: issues) {
			int count = 0;
			String name = "";
			if(issue.getStatus().isOpen()){
				count = bugCounter.getOrDefault("open", 0) + 1;
				name = "open";
			}else if(issue.getStatus().isCancelled()){
				count = bugCounter.getOrDefault("cancelled", 0) + 1;
				name = "cancelled";
			}else if(issue.getStatus().isClosed()){
				count = bugCounter.getOrDefault("closed", 0) + 1;
				name = "closed";
			}else if(issue.getStatus().isDuplicate()){
				count = bugCounter.getOrDefault("duplicate", 0) + 1;
				name = "duplicate";
			}else if(issue.getStatus().isResolved()){
				count = bugCounter.getOrDefault("resolved", 0) + 1;
				name = "resolved";
			}
			bugCounter.put(name, count);
		}
		Visualization.saveGraph(bugCounter, args[1]+bugsrcMapper.projName+"bugFileMapper.html");
	}
}
