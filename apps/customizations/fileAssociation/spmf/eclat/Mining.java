package customizations.fileAssociation.spmf.eclat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.revwalk.RevCommit;

import br.ufpe.cin.groundhog.Issue;
import ca.pfv.spmf.algorithms.frequentpatterns.eclat.AlgoEclat;
import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase;
import ca.pfv.spmf.tools.dataset_converter.TransactionDatabaseConverter;

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

	public static void main(String[] args) throws RevisionSyntaxException, IOException, GitAPIException {
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
		int totalRevs = revisions.size();
		List<String> associations = new ArrayList<>();
		BugModule bugs = new BugModule(mining.userName, mining.projName);
		List<Issue> issues = bugs.getIssues();
		for (int i = totalRevs - 1; i > 0; i--) {
			RevCommit revisionOld = revisions.get(i);
			RevCommit revisionNew = revisions.get(i - 1);
			if(bugs.isFixingRevision(revisionNew.getFullMessage(), issues)){
				List<DiffEntry> diffs = mining.git.diffsBetweenTwoRevAndChangeTypes(revisionNew, revisionOld);
				List<String> files = new ArrayList<>();
				for (DiffEntry e : diffs) {
					String path = "";
					if (e.getChangeType() == ChangeType.DELETE) {
						org.eclipse.jdt.core.dom.ASTNode node = mining.git
								.createAst(mining.git.readFile(revisionOld, e.getOldPath()));
						org.eclipse.jdt.core.dom.PackageDeclaration packageD = ((CompilationUnit)node).getPackage();
						if(packageD != null)
							  path = packageD.getName().getFullyQualifiedName();
					} else {
						org.eclipse.jdt.core.dom.ASTNode node = mining.git
								.createAst(mining.git.readFile(revisionNew, e.getNewPath()));
						org.eclipse.jdt.core.dom.PackageDeclaration packageD = ((CompilationUnit)node).getPackage();
						if(packageD != null)
						  path = packageD.getName().getFullyQualifiedName();
					}
					if(path.length() > 1)
					  files.add(path);
				}
				String filesInRev = "";
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
		saveToFile(buildArffFile(associations, mining.fileIndex), arffPath);
		try {
			performAssociation(arffPath, "/Users/nmtiwari/Desktop/temp.html");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			int count = 0;
			for (String s : entry.substring(1).split(",")) {
				if (s.length() > 1) {
					count++;
				}
			}
			if (count > 10) {
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
	
    public static void performAssociation(String arff, String path) throws java.lang.Exception {
    	TransactionDatabaseConverter converter = new TransactionDatabaseConverter();
    	String converted = path.substring(0, path.lastIndexOf('/'))+"input_converted.txt";
    	Map<Integer, String> mapping = converter.convertARFFandReturnMap(arff, converted, Integer.MAX_VALUE);
    	String output = path.substring(0, path.lastIndexOf('/')) +"/output.txt";
    	double minsup = 0.4; 
    	TransactionDatabase database = new TransactionDatabase();
    	try {
			database.loadFile(converted);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	AlgoEclat algo = new AlgoEclat();
		algo.runAlgorithm(output, database, minsup, true);
		algo.printStats();
		try {
			   String str = FileUtils.readFileToString(new File(output));
			   Visualization.saveGraph(str, path);
			 } catch (IOException e) {
			    e.printStackTrace();
			}
    }
}
