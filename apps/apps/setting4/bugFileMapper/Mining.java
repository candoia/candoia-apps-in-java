package setting4.bugFileMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;


public class Mining {
	private String projName;
	private VCSModule git;
	private HashMap<String, List<Integer>> fileBugIndex;


	public Mining(String url, String path) {
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		fileBugIndex = new HashMap<>();
		try {
			ForgeModule.clone(url, path);
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		}

		this.git = new VCSModule(path);
	}


	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		if (args.length == 2) {
			bugsrcMapper = new Mining(args[0], args[1]);
		} else {
			throw new IllegalArgumentException();
		}
		ArrayList<RevCommit> revisions = bugsrcMapper.git.getAllRevisions();
		int totalRevs = revisions.size();
		setting4.bugFileMapper.BugModule bugs = new setting4.bugFileMapper.BugModule();
		List<SVNTicket> issues = new ArrayList<>();
		try {
			issues = bugs.getIssues(bugsrcMapper.projName);
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
		Visualization.saveGraph(bugCounter, args[1] + "bug_file_mapper.html");
	}
}
