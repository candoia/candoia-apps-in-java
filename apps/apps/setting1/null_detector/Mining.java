package setting1.null_detector;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
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
		}
		Visualization.saveGraph(results, args[1] + "_nullDetector.html");
	}

	public HashMap<String, Integer> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
		HashMap<String, String> varTyp = new HashMap<String, String>();
		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.readFile(path);
				ASTNode ast = this.git.createAst(content);
				indexMap = countNullDetector(ast, indexMap, path);
			}
		}
		return indexMap;
	}



	private static HashMap<String, Integer> countNullDetector(ASTNode ast, HashMap<String, Integer> freqRecord, String filename) {
		HashSet<String> nonPrimitives = new HashSet<>();
		class NullDetector extends ASTVisitor {

			private void countWrongNullUsage(Block body){
				for(Statement s : (List<Statement>)body.statements()){
					if(s instanceof IfStatement){
						Expression cond = ((IfStatement)s).getExpression();
						if(cond instanceof  InfixExpression){
							if(((InfixExpression)cond).getOperator().equals(InfixExpression.Operator.EQUALS) || ((InfixExpression)cond).getOperator().equals(InfixExpression.Operator.NOT_EQUALS)){
								Expression lhs = ((InfixExpression)cond).getLeftOperand();
								Expression rhs = ((InfixExpression)cond).getRightOperand();
								if(lhs instanceof NullLiteral && rhs instanceof  Name && nonPrimitives.contains(((Name)rhs).getFullyQualifiedName())){
									nonPrimitives.remove(((Name)rhs).getFullyQualifiedName());
								}else if(rhs instanceof NullLiteral && lhs instanceof  Name && nonPrimitives.contains(((Name)lhs).getFullyQualifiedName())){
									nonPrimitives.remove(((Name)lhs).getFullyQualifiedName());
								}
							}
						}
					}
				}
			}
			@Override
			public boolean visit(MethodDeclaration node) {
				nonPrimitives.clear();
				List<SingleVariableDeclaration>params = node.parameters();
				for(SingleVariableDeclaration decl: params){
					if(!decl.getType().isPrimitiveType()){
						nonPrimitives.add(decl.getName().getIdentifier().toString());
					}
				}
				countWrongNullUsage(node.getBody());
				return true;
			}
		}
		NullDetector v = new NullDetector();
		ast.accept(v);
		freqRecord.put(filename, nonPrimitives.size());
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
