package setting4.bugFileMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;


/**
 * Created by nmtiwari on 7/20/16. A class for mapping the files with bugs. This
 * class lists all the files along with all the bugs which were related to some
 * change in this files. Note: Class does not check for what the change was but
 * only checks if it was in the same commit, which fixed the bug.
 */
public class Mining {
	private String projName;
	private VCSModule git;
	private String url;
	private HashMap<String, List<Integer>> fileBugIndex;

	/*
	 * url must be of form: username@url
	 */
	public Mining(String url, String path) {
		this.url = url;
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		fileBugIndex = new HashMap<>();
		try {
			ForgeModule.clone(url, path);
		} catch (IOException | GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.git = new VCSModule(path);
	}

	/*
	 * Main function for FileAssociation Mining
	 */
	public static void main(String[] args) {
		Mining bugsrcMapper = null;
		// path of the repository
		if (args.length == 2) {
			bugsrcMapper = new Mining(args[0], args[1]);
		} else {
			throw new IllegalArgumentException();
		}
		ArrayList<RevCommit> revisions = bugsrcMapper.git.getAllRevisions();
		int totalRevs = revisions.size();
		BugModule bugs = new BugModule();
		List<SVNTicket> issues = new ArrayList<>();
		try {
			issues = bugs.getIssues(bugsrcMapper.projName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			issues = bugs.getIssues(bugsrcMapper.projName);
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

		// print all the values
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
