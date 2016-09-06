package setting3.wont_fix_bugs;

import b4j.core.DefaultIssue;
import b4j.core.DefaultSearchData;
import b4j.core.Issue;
import b4j.core.session.AbstractHttpSession;
import b4j.core.session.BugzillaHttpSession;
import b4j.core.session.JiraRpcSession;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BugModule {

	public final List<Issue> getIssues(String url, String product) throws Exception {
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
}
