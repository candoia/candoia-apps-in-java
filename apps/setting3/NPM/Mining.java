package setting3.NPM;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.util.HashMap;
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
		HashMap<String, Integer> indexMap = mining.analyze();
		HashMap<String, Integer> results = new HashMap<>();
		for (String str : indexMap.keySet()) {
			System.out.println(str + " -> " + indexMap.get(str));
			if(indexMap.get(str) > 10){
				results.put(str, indexMap.get(str));	
			}
		}
		Visualization.saveGraph(results, args[1] + "_methodCallFrequency.html");
	}

	public HashMap<String, Integer> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.readFile(path);
				ASTNode ast = this.git.createAst(content);
				indexMap = countNOA(ast, indexMap);
			}
		}
		return indexMap;
	}

	private static HashMap<String, Integer> countNOA(ASTNode ast, HashMap<String, Integer> freqRecord) {
		class NOACounter extends ASTVisitor {
			@Override
			public boolean visit(TypeDeclaration node) {
				String name = node.getName().getFullyQualifiedName().toString();
				int counter = 0;
				if (freqRecord.containsKey(name)) {
					MethodDeclaration[] methods = node.getMethods();
					for(MethodDeclaration declaration: methods){
						List<Modifier> modi = declaration.modifiers();
						for(Modifier i: modi){
							if(i.isPublic())
								counter++;
						}
					}
					freqRecord.put(name, freqRecord.get(name) + counter);
				} else {
					freqRecord.put(name, counter);
				}
				return super.visit(node);
			}
		}
		NOACounter v = new NOACounter();
		ast.accept(v);
		return freqRecord;
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
