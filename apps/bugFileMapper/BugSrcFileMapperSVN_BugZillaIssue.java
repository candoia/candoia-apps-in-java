package bugFileMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import issues.ImportBugzillaReports;
import svnConnector.SVNCommit;
import svnConnector.SVNConnector;
import svnConnector.SVNRepositoryCloner;

/**
 * Created by nmtiwari on 7/20/16. A class for mapping the files with bugs. This
 * class lists all the files along with all the bugs which were related to some
 * change in this files. Note: Class does not check for what the change was but
 * only checks if it was in the same commit, which fixed the bug.
 */
public class BugSrcFileMapperSVN_BugZillaIssue {
	private SVNConnector svn;
	private String userName;
	private String projName;
	private HashMap<String, List<Integer>> fileBugIndex;
	private String bugURL;
	private String product;

	/*
	 * url must be of form: username@url
	 */
	public BugSrcFileMapperSVN_BugZillaIssue(String url, String path, String bug_url) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
//		try {
//			SVNRepositoryCloner.clone(url, path);
//		} catch (SVNException e) {
//			e.printStackTrace();
//		}
		this.svn = new SVNConnector(path);
		this.bugURL = bug_url.substring(bug_url.indexOf('@') + 1);
		this.product = bug_url.substring(0, bug_url.indexOf('@'));
	}

	/*
	 * Main function for FileAssociation Mining
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		int index = 0;
		BugSrcFileMapperSVN_BugZillaIssue bugsrcMapper = null;
		// path of the repository
		if (args.length == 3) {
			bugsrcMapper = new BugSrcFileMapperSVN_BugZillaIssue(args[0], args[1], args[2]);
		} else {
			bugsrcMapper = new BugSrcFileMapperSVN_BugZillaIssue("nmtiwari@/Users/nmtiwari/Desktop/test/pagal/panini/",
					"/Users/nmtiwari/Desktop/test/pagal/panini/svn", "Tomcat 8@https://bz.apache.org/bugzilla");
		}
		// get all the revisions of the project
		ArrayList<SVNCommit> revisions = bugsrcMapper.svn.getAllRevisions();
		int totalRevs = revisions.size();
		// get all the issues of the projects.
		ImportBugzillaReports bugs = new ImportBugzillaReports();
		List<b4j.core.Issue> issues = new ArrayList<>();
		System.out.println(bugsrcMapper.bugURL + "\n" + bugsrcMapper.product);
		try {
			issues = bugs.importBugs(bugsrcMapper.bugURL, bugsrcMapper.product);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SVNRepository repository = bugsrcMapper.svn.getRepository();

		// check all the revisions
		for (int i = 0; i < totalRevs; i++) {
			SVNCommit revision = revisions.get(i);
			// check if the revision is bug fixing revision or a simple revision
			if (bugs.isFixingRevision(revision.getMessage(), issues)) {
				// get all the files of the revisions
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

		// print all the values
		System.out.println(issues.toString());
		HashMap<String, Integer> bugCounter = new HashMap<>();
		System.out.println("Total buggy files: " + bugsrcMapper.fileBugIndex.size());
		for (String name : bugsrcMapper.fileBugIndex.keySet()) {
			int count = bugsrcMapper.fileBugIndex.get(name).size();
			System.out.println(name + " -> " + count);
			if (count > 1) {
				bugCounter.put(name, count);
			}
		}
		BugSrcFileMapperGraph.saveGraph(bugCounter, "/Users/nmtiwari/Desktop/bug.html");
	}
}
