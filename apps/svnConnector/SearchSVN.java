package svnConnector;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Guice;
import com.google.inject.Inject;

import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;
import svnConnector.URLBuilder.SVNAPI;

public class SearchSVN {
	private final Gson gson;
	private final Requests requests;
	private final URLBuilder builder;

	@Inject
	public SearchSVN(Requests requests) {
		this.requests = requests;
		this.gson = new Gson();
		this.builder = Guice.createInjector(new HttpModule()).getInstance(URLBuilder.class);
	}

	private ArrayList<SVNTicket> setTicketBuilder(String searchUrl, boolean isBug) {
		ArrayList<SVNTicket> issues = new ArrayList<>();
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
				// System.out.println(bugString);
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

	public ArrayList<SVNTicket> getTickets(String project) {
		System.out.println("Searching project tickets metadata");
		String searchUrl = builder.uses(SVNAPI.ROOT).withSimpleParam("/", project).withParam("/bugs").sbuild();
		ArrayList<SVNTicket> issues = setTicketBuilder(searchUrl, true);

//		URLBuilder frBuilder = Guice.createInjector(new HttpModule()).getInstance(URLBuilder.class);
//		String frUrl = frBuilder.uses(SVNAPI.ROOT).withSimpleParam("/", project).withParam("/feature-requests")
//				.sbuild();
//
//		setTicketBuilder(issues, frUrl, false);
//
//		URLBuilder srBuilder = Guice.createInjector(new HttpModule()).getInstance(URLBuilder.class);
//		String srUrl = srBuilder.uses(SVNAPI.ROOT).withSimpleParam("/", project).withParam("/support-requests")
//				.sbuild();
//
//		setTicketBuilder(issues, srUrl, false);

		return issues;
	}

}
