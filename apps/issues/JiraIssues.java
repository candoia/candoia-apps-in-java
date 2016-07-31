package issues;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import b4j.core.DefaultIssue;
import b4j.core.DefaultSearchData;
import b4j.core.Issue;
import b4j.core.session.*;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;


public class JiraIssues {
	private String url;
	private String project;
	private ArrayList<Issue> issues;
	
	public JiraIssues(String url) {
	  this.url = url;
	  this.issues = new ArrayList<>();
	}
	public JiraIssues(String project, String url) {
		this.project = project;
		this.url = url;
		this.issues = new ArrayList<>();
	}

	public ArrayList<Issue> importJiraIssues(){
//		final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
//		URI jiraServerUri = null;
//		final JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, "yourusername", "yourpassword");
//		final NullProgressMonitor pm = new NullProgressMonitor();
//		final com.atlassian.jira.rest.client.domain.Issue issue = restClient.getIssueClient().getIssue("", pm);

//		try {
//			jiraServerUri = new URI("https://issues.apache.org/jira");
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
		AbstractHttpSession session = new JiraRpcSession();
		try {
			((JiraRpcSession) session).setBaseUrl(new URL("https://issues.apache.org/jira"));
			session.setBugzillaBugClass(DefaultIssue.class);

			if (session.open()) {
				DefaultSearchData searchData = new DefaultSearchData();
				searchData.add("jql", "project = " + project);
				// searchData.add("jql", "project = HADOOP COMMON");
				// AND issuetype = Bug AND status in (Resolved, Closed) AND
				// resolution = Fixed
				Iterator i = session.searchBugs(searchData, null).iterator();

				while (i.hasNext()) {
//					Issue issue = (Issue) i.next();
//						issues.add(issue);
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
}
