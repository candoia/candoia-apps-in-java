package nullcheck.setting5;

import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Guice;
import setting5.nullCheck.URLBuilder.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BugModule {
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };

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
				e.printStackTrace();
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
		if (isFixingRevision(msg)) {
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

	public boolean isFixingRevision(String commitLog) {
		boolean isFixing = false;
		Pattern p;
		if (commitLog != null) {
			String tmpLog = commitLog.toLowerCase();
			for (int i = 0; i < fixingPatterns.length; i++) {
				String patternStr = fixingPatterns[i];
				p = Pattern.compile(patternStr);
				Matcher m = p.matcher(tmpLog);
				isFixing = m.find();
				if (isFixing) {
					break;
				}
			}
		}
		return isFixing;
	}

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
