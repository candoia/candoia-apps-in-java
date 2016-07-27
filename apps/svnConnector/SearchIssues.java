package svnConnector;

import br.ufpe.cin.groundhog.Issue;
import br.ufpe.cin.groundhog.IssueLabel;
import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Guice;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/*
 * A class for searching all the issues of the github repository
 */
public class SearchIssues {
    private final Gson gson;
    private final Requests requests;
    private final gitConnector.URLBuilder builder;

    @Inject
    public SearchIssues(Requests requests) {
        this.requests = requests;
        this.gson = new Gson();
        this.builder = Guice.createInjector(new HttpModule()).getInstance(gitConnector.URLBuilder.class);
    }

    /*
     * @project: groundhog project object
     * Returns a list of all the issues from the project
     */
    public List<Issue> getAllProjectIssues(Project project) {

        int pageNumber = 1;
        List<Issue> issues = new ArrayList<Issue>();

        while (true) {
            String searchUrl = builder.withParam("https://api.github.com/repos")
                    .withSimpleParam("/", project.getOwner().getLogin()).withSimpleParam("/", project.getName())
                    .withParam("/issues").withParam("?state=all&").withParam("page=" + pageNumber).build();
            String jsonString = requests.get(searchUrl);
            List<IssueLabel> lables = new ArrayList<IssueLabel>();
            if (!jsonString.equals("[]") && !jsonString.contains("\"message\":\"API rate limit exceeded for") && !jsonString.contains("bad credentials")) {
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
