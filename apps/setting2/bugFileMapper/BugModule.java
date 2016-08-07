package setting2.bugFileMapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import b4j.core.*;
import b4j.core.session.BugzillaHttpSession;
import b4j.core.Issue;

public class BugModule {
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };

	public final List<b4j.core.Issue> importBugs(String url, String product) throws Exception {
		List<b4j.core.Issue> issues = new ArrayList<>();
		BugzillaHttpSession session = new BugzillaHttpSession();
		session.setBaseUrl(new URL(url)); // https://landfill.bugzilla.org/bugzilla-tip/
		session.setBugzillaBugClass(DefaultIssue.class);
		if (session.open()) {
			DefaultSearchData searchData = new DefaultSearchData();
			searchData.add("product", product);
			Iterable<b4j.core.Issue> it = session.searchBugs(searchData, null);
			for (b4j.core.Issue issue : it) {
				issues.add(issue);
			}
			session.close();
		}
		return issues;
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
				e.printStackTrace();
			}
		}
		return ids;
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
			if ((id + "").equals(issue.getId())) {
				return true;
			}
		}
		return false;
	}
}
