package customizations.bugsrcmapper.criticalFiles;

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
import java.util.Arrays;
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
	public List<Issue> getIssueIDsFromCommitLog(String log, List<Issue> issues) {
		List<Integer> ids = getIdsFromCommitMsg(log);
		List<Issue> bugs = new ArrayList<>();
		for (Integer i : ids) {
			Issue issue = getIssueWithId(i, issues);
			if(issue != null){
				bugs.add(issue);
			}
		}
		return bugs;
	}

	public Issue getIssueWithId(Integer id, List<Issue> issues){
		for(Issue i: issues){
			if(i.getNumber() == id){
				return i;
			}
		}
		return null;
	}

	public List<Integer> getIdsFromCommitMsg(String commitLog) {
		String commitMsg = commitLog;
		commitMsg = commitMsg.replaceAll("[^0-9]+", " ");
		List<String> idAsString = Arrays.asList(commitMsg.trim().split(" "));
		List<Integer> ids = new ArrayList<Integer>();
		for (String id : idAsString) {
			try {
				if (id.trim().length() > 0 && !ids.contains(Integer.parseInt(id)))
					ids.add(Integer.parseInt(id));
			} catch (NumberFormatException e) {
				 e.printStackTrace();
			}
		}
		return ids;
	}
	
	private List<Integer> getIssueNumbers(List<Issue> issues) {
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
