package setting6.flawed_logic;


import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.Statement;


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
			if (indexMap.get(str) > 10) {
				results.put(str, indexMap.get(str));
			}

		}
		Visualization.saveGraph(results, args[1] + "_flawedLogic.html");
	}

	public HashMap<String, Integer> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();

		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.readFile(path);
				org.eclipse.wst.jsdt.core.dom.ASTNode ast = this.git.createAst(content);
				indexMap = countFlawedLogic(ast, indexMap, path.substring(path.lastIndexOf("/")+1));
			}
		}
		return indexMap;
	}


	private static HashMap<String, Integer> countFlawedLogic(org.eclipse.wst.jsdt.core.dom.ASTNode ast, HashMap<String, Integer> counts, String className) {
		class FlawedLogicDetector extends ASTVisitor {

			private int countNesting(Statement n, int counter){
				if(n instanceof IfStatement){
					Statement s = ((IfStatement) n).getThenStatement();
					int nestLevelInIf = countNesting(s, counter+1);
					s = ((IfStatement) n).getElseStatement();
					int nestLevelInElse = countNesting(s, counter+1);
					return (nestLevelInIf > nestLevelInElse ? nestLevelInIf: nestLevelInElse);
				}
				else if(n instanceof Block){
					int nestingLevel = counter;
					List<Statement> body = ((Block)n).statements();
					for(Statement s: body){
						int nesting = countNesting(s, counter);
						if(nesting > nestingLevel){
							nestingLevel = nesting;
						}
					}
					return nestingLevel;
				}else if(n instanceof DoStatement){
					int nestingLevel = counter;
					return countNesting(((DoStatement)n).getBody(), counter);
				}else if(n instanceof org.eclipse.wst.jsdt.core.dom.WhileStatement){
					int nestingLevel = counter;
					return countNesting(((org.eclipse.wst.jsdt.core.dom.WhileStatement)n).getBody(), counter);
				}else if(n instanceof EnhancedForStatement){
					int nestingLevel = counter;
					return countNesting(((EnhancedForStatement)n).getBody(), counter);
				}else if(n instanceof ForStatement){
					int nestingLevel = counter;
					return countNesting(((ForStatement)n).getBody(), counter);
				}else{
					return counter;
				}
			}
			@Override
			public boolean visit(IfStatement node) {
				int result = countNesting(node, 0);
				int currentResult = counts.getOrDefault(className, 0);
				result = result > currentResult ? result:currentResult;
				counts.put(className, result);
				return true;
			}
		}
		FlawedLogicDetector v = new FlawedLogicDetector();
		ast.accept(v);
		return counts;
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
