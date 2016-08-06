package setting2.nullCheck;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import b4j.core.*;
import b4j.core.session.BugzillaHttpSession;
import b4j.core.Issue;
import svnConnector.SVNTicket;

//import org.apache.commons.configuration.*;

public class BugModule {
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };

	public final List<b4j.core.Issue> importBugs(String url, String product) throws Exception {
		List<b4j.core.Issue> issues = new ArrayList<>();
		BugzillaHttpSession session = new BugzillaHttpSession();
		session.setBaseUrl(new URL(url)); // https://landfill.bugzilla.org/bugzilla-tip/
		session.setBugzillaBugClass(DefaultIssue.class);

		// Open the session
		if (session.open()) {
			DefaultSearchData searchData = new DefaultSearchData();
			searchData.add("product", product);

			// Perform the search
			Iterable<b4j.core.Issue> it = session.searchBugs(searchData, null);
			for (b4j.core.Issue issue : it) {
				issues.add(issue);
			}
			// Close the session
			session.close();
		}
		return issues;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BugModule b4jimporter = new BugModule();
		String url = "https://bz.apache.org/bugzilla/";
		String product = "Tomcat 8";
		try {
			b4jimporter.importBugs(url, product);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<b4j.core.Issue> getIssuesWithBuilder(String url, String product) {
		//system.out.println("product in bugzilla:"+product);
		String temp=product;
		List<b4j.core.Issue> issues = new ArrayList<>() ;
		if(product.equalsIgnoreCase("Tomcat")){
			for(int i=1;i<10;i++){
				product=temp+" "+i;

				BugModule b4jimporter = new BugModule();

				try {
					issues=b4jimporter.importBugs(url, product);
				} catch (Exception e) {
					e.printStackTrace();
				}
				//system.out.println("Total issues from bugzilla : " + issues.size());
				for (b4j.core.Issue issue : issues) {
					issues.add(issue);
				}

			}
		}
		return issues;
	}

	/*
	 * @msg: COmmit message
	 * @issues: list of all issues
	 * return boolean if this msg contains any real bug id or not
	 */
	public boolean isFixingRevision(String msg, List<b4j.core.Issue> issues) {
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
	 * @commitLog: commit message
	 * returns boolean
	 * Checks if the revision has any of the fixing patterns
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
	

	/*
	 * A method to get a list of issue numbers. Issue number is different than issue id.
     */
	public List<String> getIssueNumbers(List<b4j.core.Issue> issues) {
		List<String> ids = new ArrayList<String>();
		for (b4j.core.Issue issue : issues) {
			ids.add(issue.getId());
		}
		return ids;
	}
	
	/*
	 * A simple method which fetches all the numbers from the string.
	 * Note: It does not verify if the numbers are real bug ids or not.
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
	 * @issues: list of all issues
	 * returns a list of integers representing issue numbers.
	 * This method gives you actual issue numbers.
	 */
	public List<Integer> getIssueIDsFromCommitLog(String log, List<b4j.core.Issue> issues) {
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
	 * @issues: List of all github issues
	 * @id: integer
	 * returns if id is actual bug id or not
	 */
	private boolean isBug(List<b4j.core.Issue> issues, int id) {
		for (Issue issue : issues) {
			if ((id+"").equals(issue.getId())) {
				return true;
			}
		}
		return false;
	}
}
