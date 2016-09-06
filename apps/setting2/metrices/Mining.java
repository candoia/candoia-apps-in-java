package setting2.metrics;

import org.eclipse.jdt.core.dom.*;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import setting1.metrices.*;

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
		HashMap<String, metricsData> indexMap = mining.analyze();
		String data = "";
		for (String str : indexMap.keySet()) {
			metricsData details  = indexMap.get(str);
			data += details.nop + " " + details.noa + " " + details.nopa + " " + details.nopra + " " + details.npm + " " + details.nprm + " " + details.nprom+"\n";
		}
		Visualization.saveGraph(data, args[1] + "_metrics.html");
	}

	public HashMap<String, metricsData> analyze() {
		long startTime = System.currentTimeMillis();
		ArrayList<String> allFiles = new ArrayList<>();
		allFiles = this.svn.getAllFilesFromHead(allFiles);
		HashMap<String, metricsData> indexMap = new HashMap<String, metricsData>();

		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.svn.getFileContent(path, -1, new SVNProperties(), new ByteArrayOutputStream());
				ASTNode ast = this.svn.createAst(content);
				indexMap = countdetails(ast, indexMap);
			}
		}
		return indexMap;
	}


	private static HashMap<String, metricsData> countdetails(ASTNode ast, HashMap<String, metricsData> freqRecord) {
		class NOACounter extends ASTVisitor {
			@Override
			public boolean visit(TypeDeclaration node) {
				String name = node.getName().getFullyQualifiedName().toString();
				int noc = node.getParent().getLength();
				int noa = node.getFields().length;
				int npa = 0;
				int npra = 0;
				int npm = 0;
				int nprm = 0;
				int nprom = 0;
				FieldDeclaration[] fields = node.getFields();
				for(FieldDeclaration field: fields){
					List<Modifier> modi = field.modifiers();
					for(Modifier i: modi){
						if(i.isPublic())
							npa++;
						else if(i.isPrivate()){
							npra++;
						}
					}
				}

				MethodDeclaration[] methods = node.getMethods();
				for(MethodDeclaration declaration: methods){
					List<Modifier> modi = declaration.modifiers();
					for(Modifier i: modi){
						if(i.isPublic())
							npm++;
						else if(i.isPrivate()){
							nprm++;
						}else if(i.isProtected()){
							nprom++;
						}
					}
				}
				freqRecord.put(name, new metricsData(noc, noa, npa, npra, npm, nprm,nprom));
				return super.visit(node);
			}
		}
		NOACounter v = new NOACounter();
		ast.accept(v);
		return freqRecord;
	}
}
