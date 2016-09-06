package setting2.fileAssocaition;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import b4j.core.DefaultIssue;
import b4j.core.DefaultSearchData;
import b4j.core.session.BugzillaHttpSession;

public class BugModule {

	public final List<b4j.core.Issue> importBugs(String url, String product) throws Exception {
		List<b4j.core.Issue> issues = new ArrayList<>();
		BugzillaHttpSession session = new BugzillaHttpSession();
		session.setBaseUrl(new URL(url)); // https://landfill.bugzilla.org/bugzilla-tip/
		session.setBugzillaBugClass(DefaultIssue.class);

		if (session.open()) {
			DefaultSearchData searchData = new DefaultSearchData();
			searchData.add("product", "Tomcat 8");
			Iterable<b4j.core.Issue> it = session.searchBugs(searchData, null);
			for (b4j.core.Issue issue : it) {
				issues.add(issue);
			}
			session.close();
		}
		return issues;
	}

	public boolean isFixingRevision(String msg, List<b4j.core.Issue> issues) {
		if (VCSModule.isFixingRevision(msg)) {
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
}
