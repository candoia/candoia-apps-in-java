package fileAssociationMining;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.tmatesoft.svn.core.io.SVNRepository;

import svnConnector.SVNCommit;
import svnConnector.SVNConnector;

/**
 * Created by nmtiwari on 7/19/16. FileAssociationMining_GIT: A class for
 * getting file associations between revisions
 * 
 * @username: github username for project owner
 * @projName: project name
 */
public class FileAssociationMining_SVN {
	private SVNConnector svn;
	private String userName;
	private String projName;
	private HashMap<Integer, String> fileIndex;

	private FileAssociationMining_SVN(String repoPath) {
		this.svn = new SVNConnector(repoPath);
		String[] details = repoPath.split("/");
		this.projName = details[details.length - 1];
		this.userName = details[details.length - 2];
		this.fileIndex = new HashMap<Integer, String>();
	}

	/*
	 * url must be of form: username@url
	 */
	public FileAssociationMining_SVN(String url, String path) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		SVNConnector.cloneRepo(url, path);
		this.svn = new SVNConnector(path);
	}

	/*
	 * Main function for FileAssociation Mining This function creates a string
	 * for each revision and also create a map. In arff format every item has to
	 * be at fixed location in all occurances. Hence we need to assign a number
	 * to each file. That number represents the index for that text.
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		int index = 0;
		FileAssociationMining_SVN mining = null;
		// path of the repository
		if (args.length < 1) {
			mining = new FileAssociationMining_SVN("/Users/nmtiwari/Desktop/test/pagal/paninij");
		} else if (args.length == 2) {
			mining = new FileAssociationMining_SVN(args[1], args[0]);
		} else {
			mining = new FileAssociationMining_SVN(args[0]);
		}

		// get all revisions
		ArrayList<SVNCommit> revisions = mining.svn.getAllRevisions();
		int totalRevs = revisions.size();
		// a list of files
		List<String> associations = new ArrayList<>();

		// svn repository
		SVNRepository repository = mining.svn.getRepository();
		for (int i = 0; i < totalRevs; i++) {
			SVNCommit revision = revisions.get(i);
			String filesInRev = "";
			List<String> files = revision.getFiles();
			for (String name : files) {
				filesInRev += ("," + name);
				if (!mining.fileIndex.containsValue(name)) {
					mining.fileIndex.put(index, name);
					index++;
				}
			}
			if (filesInRev.length() > 0)
				associations.add(filesInRev.substring(1));
		}
		System.out.println(mining.fileIndex.size());
		saveToFile(buildArffFile(associations, mining.fileIndex), "/Users/nmtiwari/Desktop/output.arff");
		AprioryAssociation.runAssociation("/Users/nmtiwari/Desktop/output.arff");
	}

	// save the content in a file at given path.
	private static void saveToFile(String strContent, String fileNameAndPath) {
		BufferedWriter bufferedWriter = null;
		try {
			File myFile = new File(fileNameAndPath);
			// check if file exist, otherwise create the file before writing
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

	/*
	 * This function create an arff format for given list of text
	 */
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
