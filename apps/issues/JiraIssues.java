package issues;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import b4j.core.DefaultIssue;
import b4j.core.DefaultSearchData;
import b4j.core.Issue;
import b4j.core.session.AbstractHttpSession;
import b4j.core.session.JiraRpcSession;

public class JiraIssues {
	private String url;
	private String project;
	private ArrayList<Issue> issues;
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };

	public JiraIssues(String url) {
		this.url = url;
		this.issues = new ArrayList<>();
	}

	public JiraIssues(String project, String url) {
		this.project = project;
		this.url = url;
		this.issues = new ArrayList<>();
	}

	public ArrayList<Issue> importJiraIssues(String url) {
		AbstractHttpSession session = new JiraRpcSession();
		try {
			((JiraRpcSession) session).setBaseUrl(new URL(url));
			session.setBugzillaBugClass(DefaultIssue.class);

			if (session.open()) {
				DefaultSearchData searchData = new DefaultSearchData();
//				searchData.add("jql", "project = " + project);
				 searchData.add("jql", "project = HADOOP COMMON");
				// AND issuetype = Bug AND status in (Resolved, Closed) AND
				// resolution = Fixed
				Iterator i = session.searchBugs(searchData, null).iterator();

				while (i.hasNext()) {
					// Issue issue = (Issue) i.next();
					// issues.add(issue);
				}

				// system.out.println("Total issues : " + issues.size());
				session.close();
			}

			return issues;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			session.close();
			return issues;
		}
	}

	/*
	 * @msg: COmmit message
	 * 
	 * @issues: list of all issues return boolean if this msg contains any real
	 * bug id or not
	 */
	public boolean isFixingRevision(String msg, List<b4j.core.Issue> issues) {
		if (isFixingRevision(msg)) {
			List<String> ids = getIssueNumbers(issues);
			List<Integer> bugs = getIdsFromCommitMsg(msg);
			for (Integer i : bugs) {
				if (ids.contains(i.toString())) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * @commitLog: commit message returns boolean Checks if the revision has any
	 * of the fixing patterns
	 */
	public boolean isFixingRevision(String commitLog) {
		boolean isFixing = false;
		Pattern p;
		if (commitLog != null) {
			String tmpLog = commitLog.toLowerCase();
			for (int i = 0; i < fixingPatterns.length; i++) {
				String patternStr = fixingPatterns[i];
				p = Pattern.compile(patternStr);
				Matcher m = p.matcher(tmpLog);
				isFixing = m.find();
				if (isFixing) {
					break;
				}
			}
		}
		return isFixing;
	}

	/*
	 * A method to get a list of issue numbers. Issue number is different than
	 * issue id.
	 */
	public List<String> getIssueNumbers(List<b4j.core.Issue> issues) {
		List<String> ids = new ArrayList<String>();
		for (b4j.core.Issue issue : issues) {
			ids.add(issue.getId());
		}
		return ids;
	}

	/*
	 * A simple method which fetches all the numbers from the string. Note: It
	 * does not verify if the numbers are real bug ids or not.
	 */
	public List<Integer> getIdsFromCommitMsg(String commitLog) {
		String commitMsg = commitLog;
		commitMsg = commitMsg.replaceAll("[^0-9]+", " ");
		List<String> idAsString = Arrays.asList(commitMsg.trim().split(" "));
		List<Integer> ids = new ArrayList<Integer>();
		for (String id : idAsString) {
			try {
				if (!ids.contains(Integer.parseInt(id)))
					ids.add(Integer.parseInt(id));
			} catch (NumberFormatException e) {
				// e.printStackTrace();
			}
		}
		return ids;
	}

	/*
	 * @log: commit message
	 * 
	 * @issues: list of all issues returns a list of integers representing issue
	 * numbers. This method gives you actual issue numbers.
	 */
	public List<Integer> getIssueIDsFromCommitLog(String log, List<b4j.core.Issue> issues) {
		List<Integer> ids = getIdsFromCommitMsg(log);
		List<Integer> bugs = new ArrayList<>();
		for (Integer i : ids) {
			if (isBug(issues, i)) {
				bugs.add(i);
			}
		}
		return bugs;
	}

	/*
	 * @issues: List of all github issues
	 * 
	 * @id: integer returns if id is actual bug id or not
	 */
	private boolean isBug(List<b4j.core.Issue> issues, int id) {
		for (Issue issue : issues) {
			if ((id + "").equals(issue.getId())) {
				return true;
			}
		}
		return false;
	}
}
