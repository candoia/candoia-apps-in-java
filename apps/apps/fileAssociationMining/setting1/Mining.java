package fileAssociationMining.setting1;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import setting1.bugFileMapper.BugModule;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Mining {
	private VCSModule git;
	public String url;
	private String userName;
	private String projName;
	private HashMap<Integer, String> fileIndex;

	public Mining(String url, String path) {
		this.url = url.substring(url.indexOf('@') + 1);
		this.userName = url.substring(0, url.indexOf('@'));
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(url, path);
			} catch (GitAPIException | IOException e) {
				e.printStackTrace();
			}
		}

		this.git = new VCSModule(path);
		fileIndex = new HashMap<>();
	}

	public static void main(String[] args) {
		int index = 0;
		Mining mining = null;
		String arffPath = "";
		if (args.length == 2) {
			mining = new Mining(args[0], args[1]);
			arffPath = args[1] + "/" + mining.url.substring(mining.url.lastIndexOf('/') + 1) + ".arff";
		} else {
			throw new IllegalArgumentException();
		}
		ArrayList<RevCommit> revisions = mining.git.getAllRevisions();
		BugModule bugIds = new BugModule(mining.userName, mining.projName);
		int totalRevs = revisions.size();
		List<String> associations = new ArrayList<>();
		Repository repository = mining.git.getRepository();
		for (int i = 0; i < totalRevs; i++) {
			RevCommit revision = revisions.get(i);
			try {
				String filesInRev = "";
				List<String> files = mining.git.readElementsAt(repository, revision.getId().getName());
				for (String name : files) {
					filesInRev += ("," + name);
					if (!mining.fileIndex.containsValue(name)) {
						mining.fileIndex.put(index, name);
						index++;
					}
				}
				associations.add(filesInRev.substring(1));
			} catch (IOException e) {
				e.printStackTrace();
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
			String entry = "";
			for (Integer i : fileIndex.keySet()) {
				String fileName = fileIndex.get(i);
				if (str.contains(fileName)) {
					entry += ("," + fileName);
				} else {
					entry += (",?");
				}
			}
			br.append(entry.substring(1));
			br.append("\n");
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
