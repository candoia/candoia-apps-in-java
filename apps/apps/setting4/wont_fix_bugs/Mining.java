package setting4.wont_fix_bugs;

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
	private String projName;
	private String url;
	private HashMap<String, List<Integer>> fileBugIndex;

	/*
	 * url must be of form: username@url
	 */
	public Mining(String url, String path) {
		this.url = url;
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		fileBugIndex = new HashMap<>();
	}

	/*
	 * Main function for FileAssociation Mining
	 */
	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		HashMap<String, Integer> bugCounter = new HashMap<String, Integer>();
		if (args.length == 2) {
			bugsrcMapper = new Mining(args[0], args[1]);
		} else {
			throw new IllegalArgumentException();
		}
		BugModule bugs = new BugModule();
		List<SVNTicket> issues = new ArrayList<>();
		try {
			issues = bugs.getIssues(bugsrcMapper.projName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for(SVNTicket issue: issues){
			int count = 0;
			String name = "";
			if("open".equals(issue.getStatus())){
				count = bugCounter.getOrDefault("open", 0) +1;
				name = "open";
			}else if("open".equals(issue.getStatus())){
				count = bugCounter.getOrDefault("close", 0) +1;
				name = "close";
			}else{
				count = bugCounter.getOrDefault("wont_fix", 0) +1;
				name = "wont_fix";
			}
			bugCounter.put(name, count);
		}

		Visualization.saveGraph(bugCounter, args[1] + "bug_file_mapper.html");
	}
}
