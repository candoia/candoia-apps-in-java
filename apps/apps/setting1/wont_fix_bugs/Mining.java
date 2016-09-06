package setting1.wont_fix_bugs;

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
	private String userName;
	private String projName;
	private HashMap<String, List<Integer>> fileBugIndex;

	public Mining(String url, String path) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		this.fileBugIndex = new HashMap<>();
	}

	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		if (args.length == 2) {
			bugsrcMapper = new Mining(args[0], args[1]);
		}
		BugModule bugIds = new BugModule(bugsrcMapper.userName, bugsrcMapper.projName);
		List<Issue> issues = bugIds.getIssues();
		HashMap<String, Integer> bugCounter = new HashMap<>();
		for (Issue issue : issues) {
			if(bugCounter.containsKey(issue.getState())){
				bugCounter.put(issue.getState(), bugCounter.get(issue.getState())+1);
			}else{
				bugCounter.put(issue.getState(), 1);
			}
		}
		Visualization.saveGraph(bugCounter, args[1] + "/wontfix_" + bugsrcMapper.projName + ".html");
	}
}
