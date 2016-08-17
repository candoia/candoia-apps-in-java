package setting5.bugFileMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Guice;

import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;
import setting5.bugFileMapper.URLBuilder.SVNAPI;

public class BugModule {

	public List<String> getIssueNumbers(List<SVNTicket> issues) {
		List<String> ids = new ArrayList<String>();
		for (SVNTicket issue : issues) {
			ids.add(issue.getId());
		}
		return ids;
	}

	private boolean isBug(List<SVNTicket> issues, int id) {
		for (SVNTicket issue : issues) {
			if ((id + "").equals(issue.getId())) {
				return true;
			}
		}
		return false;
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
				// e.printStackTrace();
			}
		}
		return ids;
	}

	public List<Integer> getIssueIDsFromCommitLog(String log, List<SVNTicket> issues) {
		List<Integer> ids = getIdsFromCommitMsg(log);
		List<Integer> bugs = new ArrayList<>();
		for (Integer i : ids) {
			if (isBug(issues, i)) {
				bugs.add(i);
			}
		}
		return bugs;
	}

	public boolean isFixingRevision(String msg, List<SVNTicket> issues) {
		if (VCSModule.isFixingRevision(msg)) {
			List<String> ids = getIssueNumbers(issues);
			List<Integer> bugs = getIdsFromCommitMsg(msg);
			for (Integer i : bugs) {
				if (ids.contains(i.toString())) {
					return true;
				}
			}
		}
		return false;
	}

	public ArrayList<SVNTicket> getIssues(String user, String project) {
		ArrayList<SVNTicket> issues = new ArrayList<>();
		Requests requests = new Requests();
		URLBuilder urlbuilder = Guice.createInjector(new HttpModule()).getInstance(URLBuilder.class);
		String searchUrl = urlbuilder.uses(SVNAPI.ROOT).withSimpleParam("/", project).withParam("/bugs").sbuild();
		try {
			String jsonString = requests.get(searchUrl);
			JsonObject ticketObject = new JsonParser().parse(jsonString).getAsJsonObject();

			JsonArray jsonArray = ticketObject.get("tickets").getAsJsonArray();

			for (JsonElement element : jsonArray) {
				String ticket_num = element.getAsJsonObject().get("ticket_num").getAsString();
				SVNTicket issue = new SVNTicket(ticket_num);
				issues.add(issue);
			}

		} catch (com.google.gson.JsonSyntaxException e) {
			e.printStackTrace();
		}
		return issues;
	}
}
