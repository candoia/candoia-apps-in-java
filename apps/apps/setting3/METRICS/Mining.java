package setting3.METRICS;

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
		HashMap<String, metricsData> indexMap = mining.analyze();
		String data = "";
		for (String str : indexMap.keySet()) {
			metricsData details  = indexMap.get(str);
			data += details.nop + " " + details.noa + " " + details.nopa + " " + details.nopra + " " + details.npm + " " + details.nprm + " " + details.nprom+"\n";
		}
		Visualization.saveGraph(data, args[1] + "_npm.html");
	}

	public HashMap<String, metricsData> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		HashMap<String, metricsData> indexMap = new HashMap<String, metricsData>();
		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				String content = this.readFile(path);
				ASTNode ast = this.git.createAst(content);
				indexMap = countNPM(ast, indexMap);
			}
		}
		return indexMap;
	}

	private static HashMap<String, metricsData> countNPM(ASTNode ast, HashMap<String, metricsData> freqRecord) {
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
