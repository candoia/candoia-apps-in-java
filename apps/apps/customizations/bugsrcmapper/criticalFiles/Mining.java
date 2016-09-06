package customizations.bugsrcmapper.criticalFiles;

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
	private HashMap<String, List<Issue>> fileBugIndex;

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

	List<String> getTopNKeys(HashMap<String, List<Issue>> map, int top) {
		Set<String> keysSet = map.keySet();
		ArrayList<String> keys = new ArrayList<>(keysSet);
		Collections.sort(keys, new Comparator<String>() {

			@Override
			public int compare(String s1, String s2) {
				List<Issue> s1Issues = map.get(s1);
				List<Issue> s2Issues = map.get(s2);
				Comparator<Issue> compareIssues = new Comparator<Issue>() {
					@Override
					public int compare(Issue o1, Issue o2) {
						Date closeO1 = o1.getClosedAt();
						Date closeO2 = o2.getClosedAt();
						if (closeO1 == null && closeO2 == null) {
							return 0;
						} else if (closeO1 == null) {
							return 1;
						} else if (closeO2 == null) {
							return -1;
						}
						long s1Diff = closeO1.getTime() - o1.getCreatedAt().getTime();
						long s1Days = s1Diff / (60 * 60 * 1000);
						long s2Diff = closeO2.getTime() - o2.getCreatedAt().getTime();
						long s2Days = s2Diff / (60 * 60 * 1000);
						if (s1Days == s2Days) {
							return 0;
						} else if (s1Days < s2Days) {
							return -1;
						} else {
							return +1;
						}
					}
				};
				Collections.sort(s1Issues, compareIssues);
				Collections.sort(s2Issues, compareIssues);
				Issue i1 = s1Issues.get(0);
				Issue i2 = s2Issues.get(0);
				Date closedS1 = i1.getClosedAt();
				Date closedS2 = i2.getClosedAt();
				if (closedS1 == null && closedS2 == null) {
					return 0;
				} else if (closedS1 == null) {
					return 1;
				} else if (closedS2 == null) {
					return -1;
				}
				long s1Diff = closedS1.getTime() - i1.getCreatedAt().getTime();
				if (s1Diff < 0) {
					s1Diff = -1 * s1Diff;
				}
				long s1Days = s1Diff / (60 * 60 * 1000);
				long s2Diff = closedS2.getTime() - i2.getCreatedAt().getTime();
				if (s2Diff < 0) {
					s2Diff = -1 * s2Diff;
				}
				long s2Days = s2Diff / (60 * 60 * 1000);
				if (s1Days == s2Days) {
					return 0;
				} else if (s1Days < s2Days) {
					return -1;
				} else {
					return +1;
				}

			}
		});
		return keys.subList(0, top);
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
							if (!bugsrcMapper.fileBugIndex.containsKey(name)) {
								bugsrcMapper.fileBugIndex.put(name, bugs);
							} else {
								List<Issue> alreadyAssigned = bugsrcMapper.fileBugIndex.get(name);
								for (Issue bug : bugs) {
									if (!alreadyAssigned.contains(bug)) {
										alreadyAssigned.add(bug);
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
		for (String name : bugsrcMapper.getTopNKeys(bugsrcMapper.fileBugIndex, 50)) {
			int count = bugsrcMapper.fileBugIndex.get(name).size();
			bugCounter.put(name, count);
		}
		Visualization.saveGraph(bugCounter, args[1] + "/bugSrcMapper_" + bugsrcMapper.projName + ".html");
	}
}
