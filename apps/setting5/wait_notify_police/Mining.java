package setting5.wait_notify_police;

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
}
