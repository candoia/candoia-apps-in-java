package setting2.wont_fix_bugs;

import b4j.core.DefaultIssue;
import b4j.core.DefaultSearchData;
import b4j.core.Issue;
import b4j.core.session.BugzillaHttpSession;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BugModule {

	public final List<Issue> getIssues(String url, String product) throws Exception {
		List<Issue> issues = new ArrayList<>();
		BugzillaHttpSession session = new BugzillaHttpSession();
		session.setBaseUrl(new URL(url)); // https://landfill.bugzilla.org/bugzilla-tip/
		session.setBugzillaBugClass(DefaultIssue.class);
		if (session.open()) {
			DefaultSearchData searchData = new DefaultSearchData();
			searchData.add("product", product);
			Iterable<Issue> it = session.searchBugs(searchData, null);
			for (Issue issue : it) {
				issues.add(issue);
			}
			session.close();
		}
		return issues;
	}
}
