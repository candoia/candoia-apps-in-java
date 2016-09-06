package setting5.static_not_final;

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
				indexMap = countSerializationViolations(ast, indexMap);
			}
		}
		return indexMap;
	}


	private static HashMap<String, Integer> countSerializationViolations(ASTNode ast, HashMap<String, Integer> freqRecord) {
		class SerializationViolator extends ASTVisitor {
			@Override
			public boolean visit(TypeDeclaration node) {
				String name = node.getName().getFullyQualifiedName().toString();
				for(FieldDeclaration decl: node.getFields()){
					List<Modifier> modi = decl.modifiers();
					for(Modifier mod: modi){
						if(mod.isStatic()){
							if(!mod.isFinal()){
								List< VariableDeclarationFragment> vars = decl.fragments();
								for( VariableDeclarationFragment v: vars){
										if (freqRecord.containsKey(name)) {
											freqRecord.put(name, freqRecord.get(name) + 1);
										} else {
											freqRecord.put(name, 1);
										}
								}
							}
						}
					}
				}

				return super.visit(node);
			}
		}
		SerializationViolator v = new SerializationViolator();
		ast.accept(v);
		return freqRecord;
	}
}
