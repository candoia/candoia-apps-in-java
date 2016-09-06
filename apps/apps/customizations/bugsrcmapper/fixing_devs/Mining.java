package customizations.bugsrcmapper.fixing_devs;

import br.ufpe.cin.groundhog.Issue;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
	private HashMap<String, List<String>> fileBugIndex;

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

	public HashMap<String, ArrayList<String>> getModuleDevCounter(HashMap<String, List<Issue>> file_issues) {
		HashMap<String, ArrayList<String>> devCounter = new HashMap<>();
		for (String file : file_issues.keySet()) {
			String module = file;
			int index = file.lastIndexOf('/');
			if (index > -1) {
				module = file.substring(0, index);
			}
			ArrayList<String> devs = devCounter.get(module);
			if (devs == null) {
				devs = new ArrayList<String>();
			}
			for (Issue issue : file_issues.get(file)) {
				if (!devs.contains(issue.getClosedBy().getName())) {
					devs.add(issue.getClosedBy().getName());
				}
			}
			devCounter.put(module, devs);
		}
		return devCounter;
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
					List<Issue> bugs = bugIds.getIssueIDsFromCommitLog(revision.getFullMessage(), issues);
					for (String name : files) {
						if (name.contains(".") && !name.endsWith(".jar")) {
							if (name.contains("/")) {
								name = name.substring(0, name.lastIndexOf('/'));
							}
							for (Issue bug : bugs) {
								if (!bugsrcMapper.fileBugIndex.containsKey(name)) {
									if (bug.getClosedBy() != null) {
										ArrayList<String> fixedBy = new ArrayList<>();
										fixedBy.add(bug.getClosedBy().getLogin());
										bugsrcMapper.fileBugIndex.put(name, fixedBy);
									}
								} else {
									List<String> alreadyAssigned = bugsrcMapper.fileBugIndex.get(name);
									if (bug.getClosedBy() != null
											&& !alreadyAssigned.contains(bug.getClosedBy().getLogin())) {
										alreadyAssigned.add(bug.getClosedBy().getLogin());
									}
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
		for (String module : bugsrcMapper.fileBugIndex.keySet()) {
			int count = bugsrcMapper.fileBugIndex.get(module).size();
			bugCounter.put(module, count);
		}
		Visualization.saveGraph(bugCounter, args[1] + "/bugSrcMapper_" + bugsrcMapper.projName + ".html");
	}
}
