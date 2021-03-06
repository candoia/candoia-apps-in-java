package fileAssociationMining.setting1;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Guice;
import br.ufpe.cin.groundhog.Issue;
import br.ufpe.cin.groundhog.IssueLabel;
import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.User;
import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;

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
	private boolean isBug(List<Issue> issues, int id) {
		for (Issue issue : issues) {
			if (id == issue.getNumber()) {
				return true;
			}
		}
		return false;
	}
	public List<Integer> getIssueIDsFromCommitLog(String log, List<Issue> issues) {
		List<Integer> ids = getIdsFromCommitMsg(log);
		List<Integer> bugs = new ArrayList<>();
		for (Integer i : ids) {
			if (isBug(issues, i)) {
				bugs.add(i);
			}
		}
		return bugs;
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
				 //e.printStackTrace();
			}
		}
		return ids;
	}
	public List<Integer> getIssueNumbers(List<Issue> issues) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Issue issue : issues) {
			ids.add(issue.getNumber());
		}
		return ids;
	}

	public boolean isFixingRevision(String msg, List<Issue> issues) {
		if (VCSModule.isFixingRevision(msg)) {
			List<Integer> ids = getIssueNumbers(issues);
			List<Integer> bugs = getIdsFromCommitMsg(msg);
			for (Integer i : bugs) {
				if (ids.contains(i)) {
					return true;
				}
			}
		}
		return false;
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
				} catch (java.lang.ClassCastException e) {
					JsonElement element = gson.fromJson(jsonString, JsonElement.class);
					Issue issue = gson.fromJson(element, Issue.class);
					issue.setProject(project);
					try {
						for (JsonElement lab : element.getAsJsonObject().get("labels").getAsJsonArray()) {
							IssueLabel label = gson.fromJson(lab, IssueLabel.class);
							lables.add(label);
						}

					} catch (java.lang.NullPointerException ex) {
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
