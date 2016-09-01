package setting2.fileAssocaition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;

import setting1.fileAssociationMining.AprioryAssociation;
import setting2.nullCheck.BugModule;

public class Mining {
	public String url;
	private String userName;
	private String projName;
	private VCSModule svn;
	private String bugURL;
	private String product;
	private HashMap<Integer, String> fileIndex;

	public Mining(String url, String path, String bug_url) {
		this.url = url.substring(url.indexOf('@') + 1);
		this.userName = url.substring(0, url.indexOf('@'));
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		url = url.substring(url.indexOf('@') + 1);
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(url, path);
			} catch (SVNException e) {
				e.printStackTrace();
			}
		}
		this.svn = new VCSModule(path);
		this.fileIndex = new HashMap<>();
		this.bugURL = bug_url.substring(bug_url.indexOf('@') + 1);
		this.product = bug_url.substring(0, bug_url.indexOf('@'));
	}

	public static void main(String[] args) {
		int index = 0;
		Mining mining = null;
		String arffPath = "";
		if (args.length == 3) {
			mining = new Mining(args[0], args[1], args[2]);
			arffPath = args[1] + "/" + mining.url.substring(mining.url.lastIndexOf('/') + 1) + ".arff";
		} else {
			throw new IllegalArgumentException();
		}
		ArrayList<SVNCommit> revisions = mining.svn.getAllRevisions();
		int totalRevs = revisions.size();
		List<String> associations = new ArrayList<>();
		BugModule bugs = new BugModule();
		List<b4j.core.Issue> issues = new ArrayList<>();
		try {
			issues = bugs.importBugs(mining.bugURL, mining.product);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = totalRevs - 1; i > 0; i--) {
			SVNCommit revisionOld = revisions.get(i);
			SVNCommit revisionNew = revisions.get(i - 1);
			if (bugs.isFixingRevision(revisionNew.getMessage(), issues)) {
				ArrayList<SVNLogEntry> diffs = mining.svn.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
				String filesInRev = "";
				List<String> files = new ArrayList<String>();
				for (SVNLogEntry entry : diffs) {
					for (String k : entry.getChangedPaths().keySet()) {
						files.add(entry.getChangedPaths().get(k).getPath());
					}
					for (String name : files) {
						filesInRev += ("," + name);
						if (!mining.fileIndex.containsValue(name)) {
							mining.fileIndex.put(index, name);
							index++;
						}
					}
					if (filesInRev.length() > 0) {
						associations.add(filesInRev.substring(1));
					}
				}
			}
		}
		saveToFile(buildArffFile(associations, mining.fileIndex), arffPath);
		AprioryAssociation.runAssociation(arffPath);
	}

	private static void saveToFile(String strContent, String fileNameAndPath) {
		BufferedWriter bufferedWriter = null;
		try {
			File myFile = new File(fileNameAndPath);
			if (!myFile.exists()) {
				myFile.createNewFile();
			}
			Writer writer = new FileWriter(myFile);
			bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(strContent);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedWriter != null)
					bufferedWriter.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private static String buildArffFile(List<String> text, HashMap<Integer, String> fileIndex) {
		StringBuilder br = new StringBuilder();
		br.append(buildArffheader(fileIndex.size()));
		for (String str : text) {
			int counter = 0;
			String entry = "";
			for (Integer i : fileIndex.keySet()) {
				String fileName = fileIndex.get(i);
				if (str.contains(fileName)) {
					entry += ("," + fileName);
					counter++;
				} else {
					entry += (",?");
				}
			}
			if (counter > 6) {
				br.append(entry.substring(1));
				br.append("\n");
			}
		}
		return br.toString();
	}

	private static String buildArffheader(int size) {
		StringBuilder br = new StringBuilder();
		br.append("@RELATION FileCoupling\n");
		for (int index = 0; index < size; index++) {
			br.append("@ATTRIBUTE file");
			br.append(index);
			br.append(" string\n");
		}
		br.append("@DATA\n");
		return br.toString();
	}
}
