package setting3.convention_checker;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Mining {
	private VCSModule git;
	public String url;

	private Mining(String url, String path) {
		this.url = url;
		url = url.substring(url.indexOf('@') + 1);
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(url, path);
			} catch (IOException | GitAPIException e) {
				e.printStackTrace();
			}
		}
		this.git = new VCSModule(path);
	}

	public static void main(String[] args) {
		Mining mining = new Mining(args[0], args[1]);
		ArrayList<String> classes = mining.analyze();
		String results = "";
		for(String name: classes){
			results = name+"\n";
		}
		Visualization.saveGraph(results, args[1] + "_methodCallFrequency.html");
	}

	public ArrayList<String> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		ArrayList<String> classNames = new ArrayList<>();
		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.readFile(path);
				ASTNode ast = this.git.createAst(content);
				classNames = conventionChecker(ast, classNames);
			}
		}
		return classNames;
	}

	private static ArrayList<String> conventionChecker(ASTNode ast, ArrayList<String> classNames) {
		class Convention extends ASTVisitor {
			@Override
			public boolean visit(TypeDeclaration node) {
				String name = node.getName().getFullyQualifiedName().toString();
				if(Character.isLowerCase(name.charAt(0))){
					classNames.add(name);
				}
				return true;
			}
		}
		Convention v = new Convention();
		ast.accept(v);
		return classNames;
	}

	private String readFile(String path) {
		String line = null;
		String content = "";
		try {
			FileReader fileReader = new FileReader(path);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			while ((line = bufferedReader.readLine()) != null) {
				content += line;
			}
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + path + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + path + "'");
		}
		return content;
	}
}
