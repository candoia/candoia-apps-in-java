package setting4.wont_fix_bugs;

import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Guice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BugModule {
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };

	public ArrayList<SVNTicket> getIssues(String project) {
		ArrayList<SVNTicket> issues = new ArrayList<>();
		Requests requests = new Requests();
		URLBuilder urlbuilder = Guice.createInjector(new HttpModule()).getInstance(URLBuilder.class);
		String searchUrl = urlbuilder.uses(URLBuilder.SVNAPI.ROOT).withSimpleParam("/", project).withParam("/bugs").sbuild();
		try {
			String jsonString = requests.get(searchUrl);
			JsonObject ticketObject = new JsonParser().parse(jsonString).getAsJsonObject();
			JsonArray jsonArray = ticketObject.get("tickets").getAsJsonArray();
			for (JsonElement element : jsonArray) {
				String ticket_num = element.getAsJsonObject().get("ticket_num").getAsString();
				SVNTicket issue = new SVNTicket(ticket_num);
				URLBuilder builder = Guice.createInjector(new HttpModule()).getInstance(URLBuilder.class);
				String bugUrl = builder.withParam(searchUrl).withSimpleParam("/", ticket_num).sbuild();
				String bugString = requests.get(bugUrl);
				JsonObject jObj = new JsonParser().parse(bugString).getAsJsonObject();
				JsonElement ticket = jObj.get("ticket").getAsJsonObject();
				// issueBuilder.setKind(IssueKind.BUG);
				issue.setSummary(ticket.getAsJsonObject().get("summary").getAsString());
				issue.setDescription(ticket.getAsJsonObject().get("description").getAsString());
				String status = ticket.getAsJsonObject().get("status").getAsString();
				String assigned_to_id = null;
				if (!ticket.getAsJsonObject().get("assigned_to_id").isJsonNull())
					assigned_to_id = ticket.getAsJsonObject().get("assigned_to_id").getAsString();
				issues.add(issue);
			}
		} catch (com.google.gson.JsonSyntaxException e) {
			e.printStackTrace();
		}
		return issues;
	}
}
