package setting1.static_not_final;

import org.eclipse.jdt.core.dom.*;
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
		Visualization.saveGraph(results, args[1] + "_static_not_final.html");
	}

	public HashMap<String, Integer> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.readFile(path);
				ASTNode ast = this.git.createAst(content);
				indexMap = staticNotFinal(ast, indexMap);
			}
		}
		return indexMap;
	}

	private static HashMap<String, Integer> staticNotFinal(ASTNode ast, HashMap<String, Integer> freqRecord) {
		class StaticNotFinal extends ASTVisitor {
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
		StaticNotFinal v = new StaticNotFinal();
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
