package setting3.bugFileMapper;

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

public class BugModule{
	static int ids = 0;
	private String url;
	private String product;
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };
	
	public BugModule(String url, String product){
		this.url = url;
		this.product = product;
	}

	public final List<Issue> importJiraIssues() {
		List<Issue> issues = new ArrayList<>();
		AbstractHttpSession session = new JiraRpcSession();
		try {
			((JiraRpcSession) session).setBaseUrl(new URL(url));
			session.setBugzillaBugClass(DefaultIssue.class);

			if (session.open()) {
				DefaultSearchData searchData = new DefaultSearchData();
				searchData.add("jql", "project = " + product);
				searchData.add("limit", "0");
				searchData.add("issuetype", "bug");
				searchData.add("status", "Resolved");
				Iterator i = session.searchBugs(searchData, null).iterator();

				while (i.hasNext()) {
					Issue issue = (Issue) i.next();
					System.out.println(issue.getId());
					;
					try {
						issues.add(issue);
					} catch (Exception e) {
						System.out.println("Exception found in issue : " + issue.getId());
					}
				}
				session.close();
			}

			return issues;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			session.close();
			return new ArrayList<>();
		}
	}


	public static void main(String[] args) {
		BugModule jira = new BugModule("https://issues.apache.org/jira/","HADOOP");
		jira.importJiraIssues();
	}

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
	
	private boolean isBug(List<b4j.core.Issue> issues, int id) {
		for (Issue issue : issues) {
			if ((id+"").equals(issue.getId())) {
				return true;
			}
		}
		return false;
	}
	
	public List<String> getIssueNumbers(List<b4j.core.Issue> issues) {
		List<String> ids = new ArrayList<String>();
		for (b4j.core.Issue issue : issues) {
			ids.add(issue.getId());
		}
		return ids;
	}
	
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
}
