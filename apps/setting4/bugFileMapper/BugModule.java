package setting4.bugFileMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Guice;

import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.http.Requests;
import setting4.bugFileMapper.URLBuilder.SVNAPI;

public class BugModule{
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };

	/*
	 * A method to get a list of issue numbers. Issue number is different than
	 * issue id.
	 */
	public List<String> getIssueNumbers(List<SVNTicket> issues) {
		List<String> ids = new ArrayList<String>();
		for (SVNTicket issue : issues) {
			ids.add(issue.getId());
		}
		return ids;
	}
	
	
	/*
	 * @issues: List of all github issues
	 * 
	 * @id: integer returns if id is actual bug id or not
	 */
	private boolean isBug(List<SVNTicket> issues, int id) {
		for (SVNTicket issue : issues) {
			if ((id + "").equals(issue.getId())) {
				return true;
			}
		}
		return false;
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
	 * @log: commit message
	 * 
	 * @issues: list of all issues returns a list of integers representing issue
	 * numbers. This method gives you actual issue numbers.
	 */
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
	
	
	/*
	 * @msg: COmmit message
	 * 
	 * @issues: list of all issues return boolean if this msg contains any real
	 * bug id or not
	 */
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
	
	/*
	 * @commitLog: commit message returns boolean Checks if the revision has any
	 * of the fixing patterns
	 */
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
}
