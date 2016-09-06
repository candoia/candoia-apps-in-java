package setting5.convention_checker;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;


public class Mining {
	private VCSModule svn;

	private Mining(String url, String path) {
		url = url.substring(url.indexOf('@') + 1);
		if (!new File(path).isDirectory()) {
			try {
				ForgeModule.clone(url, path);
			} catch (SVNException e) {
				e.printStackTrace();
			}
		}

		this.svn = new VCSModule(path);
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
		ArrayList<String> allFiles = new ArrayList<>();
		allFiles = this.svn.getAllFilesFromHead(allFiles);
		ArrayList<String> classNames = new ArrayList<>();
		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.svn.getFileContent(path, -1, new SVNProperties(), new ByteArrayOutputStream());
				ASTNode ast = this.svn.createAst(content);
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
}
