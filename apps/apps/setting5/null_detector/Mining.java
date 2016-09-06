package setting5.null_detector;

import org.eclipse.jdt.core.dom.*;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
		Visualization.saveGraph(indexMap, args[1] + mining.url + "_null_detector.html");
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
				indexMap = countMethodCallFreq(ast, indexMap, path);
			}
		}
		return indexMap;
	}


	private static HashMap<String, Integer> countMethodCallFreq(ASTNode ast, HashMap<String, Integer> freqRecord, String filename) {
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
}
