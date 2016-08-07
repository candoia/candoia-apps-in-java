package setting6.bugFileMapper;

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
import com.google.inject.Injector;

import br.ufpe.cin.groundhog.Issue;
import br.ufpe.cin.groundhog.IssueLabel;
import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.User;
import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;
import br.ufpe.cin.groundhog.search.SearchModule;

public class BugModule {
	private List<Issue> _issues;
	private Project project;
	private String user;
	private String proj;
	private final Gson gson;
	private final URLBuilder builder;
	private final Requests requests;

	public BugModule(String username, String projName) {
		Injector injector = Guice.createInjector(new SearchModule());
		this._issues = new ArrayList<>();
		User user = new User(username);
		this.project = new Project(user, projName);
		this.project = new Project(user, projName);
		this.user = username;
		this.proj = projName;
		this.requests = new Requests();
		this.gson = new Gson();
		this.builder = Guice.createInjector(new HttpModule()).getInstance(URLBuilder.class);
	}

	/*
	 * This method reads the user password using java.util.Console service. In
	 * some IDE's like eclipse this service is not available and creation of
	 * console form the IDE always return null. In that very case password is
	 * read using normal BufferedReader and input is visible as the user types
	 * in. This is not bug but a feature not supported in some IDEs.
	 */
	public static char[] readPassword() {
		char[] pwd = null;
		if (pwd != null) {
			return pwd;
		} else {
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
	}

	private static String readLine() throws IOException {
		if (System.console() != null) {
			return System.console().readLine();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		return reader.readLine();
	}

	/*
	 * @issues: List of all github issues
	 * 
	 * @id: integer returns if id is actual bug id or not
	 */
	private boolean isBug(List<Issue> issues, int id) {
		for (Issue issue : issues) {
			if (id == issue.getNumber()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * @log: commit message
	 * 
	 * @issues: list of all issues returns a list of integers representing issue
	 * numbers. This method gives you actual issue numbers.
	 */
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
	 * A method to get a list of issue numbers. Issue number is different than
	 * issue id.
	 */
	public List<Integer> getIssueNumbers(List<Issue> issues) {
		List<Integer> ids = new ArrayList<Integer>();
		for (Issue issue : issues) {
			ids.add(issue.getNumber());
		}
		return ids;
	}

	/*
	 * @msg: COmmit message
	 * 
	 * @issues: list of all issues return boolean if this msg contains any real
	 * bug id or not
	 */
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

		while (true) {
			String searchUrl = builder.withParam("https://api.github.com/repos")
					.withSimpleParam("/", project.getOwner().getLogin()).withSimpleParam("/", project.getName())
					.withParam("/issues").withParam("?state=all&").withParam("page=" + pageNumber).build();
			String jsonString = this.requests.get(searchUrl);
			List<IssueLabel> lables = new ArrayList<IssueLabel>();
			if (!jsonString.equals("[]") && !jsonString.contains("\"message\":\"API rate limit exceeded for")
					&& !jsonString.contains("bad credentials")) {
				if (pageNumber % 10 == 0)
					System.out.println("page:" + pageNumber);
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
