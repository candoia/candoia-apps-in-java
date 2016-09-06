package setting6.NOA;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
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
		Visualization.saveGraph(results, args[1] + "_noa.html");
	}

	public HashMap<String, Integer> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
		for (String path : allFiles) {
			if (path.endsWith(".js")) {
				String content = this.readFile(path);
				org.eclipse.wst.jsdt.core.dom.ASTNode ast = this.git.createAst(content);
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
				if (freqRecord.containsKey(name)) {
					freqRecord.put(name, freqRecord.get(name) + node.getFields().length);
				} else {
					freqRecord.put(name, node.getFields().length);
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
