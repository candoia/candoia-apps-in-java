package setting2.flawed_logic;

import org.eclipse.jdt.core.dom.*;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Mining {
	private VCSModule svn;
	private String url;

	private Mining(String url, String path) {
		this.url = url;
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
		HashMap<String, Integer> indexMap = mining.analyze();
		HashMap<String, Integer> updaedIndexMap = new HashMap<>();
		for (String str : indexMap.keySet()) {
			if (indexMap.get(str) > 15) {
				updaedIndexMap.put(str.replaceAll("\n", ""), indexMap.get(str));
			}
		}
		Visualization.saveGraph(updaedIndexMap, args[1] + mining.url + "_flawedLogic.html");
	}

	public HashMap<String, Integer> analyze() {
		long startTime = System.currentTimeMillis();
		ArrayList<String> allFiles = new ArrayList<>();
		allFiles = this.svn.getAllFilesFromHead(allFiles);
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();

		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.svn.getFileContent(path, -1, new SVNProperties(), new ByteArrayOutputStream());
				ASTNode ast = this.svn.createAst(content);
				indexMap = countFlawedLogic(ast, indexMap, path.substring(path.lastIndexOf("/")+1));
			}
		}
		return indexMap;
	}


	private static HashMap<String, Integer> countFlawedLogic(ASTNode ast, HashMap<String, Integer> counts, String className) {
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
				}else if(n instanceof WhileStatement){
					int nestingLevel = counter;
					return countNesting(((WhileStatement)n).getBody(), counter);
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
}
