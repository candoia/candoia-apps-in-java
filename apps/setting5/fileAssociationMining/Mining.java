package setting5.fileAssociationMining;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nmtiwari on 7/19/16. FileAssociationMining_GIT: A class for
 * getting file associations between revisions
 * 
 * @username: github username for project owner
 * @projName: project name
 */
public class Mining {
	public String url;
	private VCSModule svn;
	private HashMap<Integer, String> fileIndex;

	/*
	 * url must be of form: username@url
	 */
	public Mining(String url, String path) {
		this.url = url.substring(url.indexOf('@') + 1);
		url = url.substring(url.indexOf('@') + 1);
		if (!new File(path).isDirectory())
		  VCSModule.cloneRepo(url, path);
		this.svn = new VCSModule(path);
		this.fileIndex = new HashMap<>();
	}

	/*
	 * Main function for FileAssociation Mining This function creates a string
	 * for each revision and also create a map. In arff format every item has to
	 * be at fixed location in all occurances. Hence we need to assign a number
	 * to each file. That number represents the index for that text.
	 */
	public static void main(String[] args) {
		int index = 0;
		Mining mining = null;
		String arffPath = "";
		// path of the repository
		if (args.length == 2) {
			mining = new Mining(args[0], args[1]);
			arffPath = args[1] + "/" + mining.url.substring(mining.url.lastIndexOf('/') + 1) + ".arff";
		} else {
			throw new IllegalArgumentException();
		}

		// get all revisions
		ArrayList<SVNCommit> revisions = mining.svn.getAllRevisions();
		int totalRevs = revisions.size();
		// a list of files
		List<String> associations = new ArrayList<>();

		// svn repository
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
		saveToFile(buildArffFile(associations, mining.fileIndex), arffPath);
		AprioryAssociation.runAssociation(arffPath);
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
