package setting1.wait_notify_police;

import japa.parser.ast.expr.MethodCallExpr;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.core.builder.State;
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
		class WaitNotifyDetector extends ASTVisitor {
			private void countWrongWaitnotify(Block body){
				int counter = 0;
				for(Statement s : (List<Statement>)body.statements()){
					if(s instanceof  SynchronizedStatement){
						SynchronizedStatement sync = (SynchronizedStatement) s;
						Block syncBody = sync.getBody();
						List<Statement> syncStmts = syncBody.statements();
						for(Statement syncst: syncStmts) {
							if(syncst instanceof    ExpressionStatement){
								ExpressionStatement exp = (ExpressionStatement)syncst;
								Expression expr = exp.getExpression();
								if(expr instanceof   MethodInvocation){
									MethodInvocation method = (MethodInvocation) expr;
									String mname = method.getName().getFullyQualifiedName();
									if("wait".equals(mname)){
										counter++;
									}
								}
							}
						}
					}
				}
				int record =  freqRecord.getOrDefault(filename, 0)+ counter;
				freqRecord.put(filename, record);
			}
			@Override
			public boolean visit(WhileStatement node) {
				WhileStatement stmt = (WhileStatement)node;
				Statement body = stmt.getBody();
				if(body instanceof Block){
					countWrongWaitnotify((Block)body);
				}
				return true;
			}
		}
		WaitNotifyDetector v = new WaitNotifyDetector();
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
