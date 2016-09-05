package setting1.wont_fix_bugs;

import br.ufpe.cin.groundhog.Issue;
import br.ufpe.cin.groundhog.IssueLabel;
import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.User;
import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Guice;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BugModule {
	private Project project;
	private final URLBuilder builder;

	public BugModule(String username, String projName) {
		User user = new User(username);
		this.project = new Project(user, projName);
		this.project = new Project(user, projName);
		this.builder = Guice.createInjector(new HttpModule()).getInstance(URLBuilder.class);
	}

	public static char[] readPassword() {
		char[] pwd = null;
			Console cnsl = null;
			char[] password = null;
			try {
				cnsl = System.console();
				if (cnsl != null) {
					password = cnsl.readPassword("Password: ");
				} else {
					return readLine().toCharArray();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			pwd = password;
			return password;
	}

	private static String readLine() throws IOException {
		if (System.console() != null) {
			return System.console().readLine();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		return reader.readLine();
	}

	public List<Issue> getIssues() {
		int pageNumber = 1;
		List<Issue> issues = new ArrayList<Issue>();
		Gson gson = new Gson();
		while (true) {
			String searchUrl = builder.withParam("https://api.github.com/repos")
					.withSimpleParam("/", project.getOwner().getLogin()).withSimpleParam("/", project.getName())
					.withParam("/issues").withParam("?state=all&").withParam("page=" + pageNumber).build();
			String jsonString = new Requests().get(searchUrl);
			List<IssueLabel> lables = new ArrayList<IssueLabel>();
			if (!jsonString.equals("[]") && !jsonString.contains("\"message\":\"API rate limit exceeded for")
					&& !jsonString.contains("bad credentials")) {
				try {
					JsonArray jsonArray = gson.fromJson(jsonString, JsonArray.class);

					for (JsonElement element : jsonArray) {
						Issue issue = gson.fromJson(element, Issue.class);
						issue.setProject(project);
						for (JsonElement lab : element.getAsJsonObject().get("labels").getAsJsonArray()) {
							IssueLabel label = gson.fromJson(lab, IssueLabel.class);
							lables.add(label);
						}
						issue.setLabels(lables);
						issues.add(issue);
						lables.clear();
					}
				} catch (ClassCastException e) {
					JsonElement element = gson.fromJson(jsonString, JsonElement.class);
					Issue issue = gson.fromJson(element, Issue.class);
					issue.setProject(project);
					try {
						for (JsonElement lab : element.getAsJsonObject().get("labels").getAsJsonArray()) {
							IssueLabel label = gson.fromJson(lab, IssueLabel.class);
							lables.add(label);
						}

					} catch (NullPointerException ex) {
						ex.printStackTrace();
					}
					issue.setLabels(lables);
					issues.add(issue);
					lables.clear();
				}
				pageNumber++;
			} else {
				break;
			}
		}
		return issues;
	}
}
