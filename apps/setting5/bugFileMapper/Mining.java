package setting5.bugFileMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.tmatesoft.svn.core.SVNException;


public class Mining {
	private String projName;
	private VCSModule svn;
	private String url;
	private HashMap<String, List<Integer>> fileBugIndex;

	public Mining(String url, String path) {
		this.url = url;
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		fileBugIndex = new HashMap<>();
		if(!new File(path).isDirectory()){
			try {
				ForgeModule.clone(url.substring(url.indexOf('@') + 1), path);
			} catch (SVNException e) {
				e.printStackTrace();
			}
		}

		this.svn = new VCSModule(path);
	}

	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		// path of the repository
		if (args.length == 2) {
			bugsrcMapper = new Mining(args[0], args[1]);
		} else {
			throw new IllegalArgumentException();
		}
		ArrayList<SVNCommit> revisions = bugsrcMapper.svn.getAllRevisions();
		int totalRevs = revisions.size();
		BugModule bugs = new BugModule();
		List<SVNTicket> issues = new ArrayList<>();
		try {
			issues = bugs.getIssues(bugsrcMapper.projName);
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
