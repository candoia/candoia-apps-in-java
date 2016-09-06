package setting2.NPM;

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
		Visualization.saveGraph(updaedIndexMap, args[1] + mining.url + "_methodusage.html");
	}

	public HashMap<String, Integer> analyze() {
		long startTime = System.currentTimeMillis();
		ArrayList<String> allFiles = new ArrayList<>();
		allFiles = this.svn.getAllFilesFromHead(allFiles);
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
		HashMap<String, String> varTyp = new HashMap<String, String>();

		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.svn.getFileContent(path, -1, new SVNProperties(), new ByteArrayOutputStream());
				ASTNode ast = this.svn.createAst(content);
				indexMap = countNPM(ast, indexMap);
			}
		}
		return indexMap;
	}


	private static HashMap<String, Integer> countNPM(ASTNode ast, HashMap<String, Integer> freqRecord) {
		class NOACounter extends ASTVisitor {
			@Override
			public boolean visit(TypeDeclaration node) {
				String name = node.getName().getFullyQualifiedName().toString();
				int counter = 0;
				if (freqRecord.containsKey(name)) {
					MethodDeclaration[] methods = node.getMethods();
					for(MethodDeclaration declaration: methods){
						List<Modifier> modi = declaration.modifiers();
						for(org.eclipse.jdt.core.dom.Modifier i: modi){
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
}
