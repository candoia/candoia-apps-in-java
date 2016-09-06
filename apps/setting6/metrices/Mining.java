package setting6.metrics;


import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.wst.jsdt.core.ast.IAbstractFunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;


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
		Visualization.saveGraph(data, args[1] + "_metrics.html");
	}

	public HashMap<String, metricsData> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		HashMap<String, metricsData> indexMap = new HashMap<String, metricsData>();
		for (String path : allFiles) {
			if (path.endsWith(".js")) {
				String content = this.readFile(path);
				ASTNode ast = this.git.createAst(content);
				indexMap = countDetails(ast, indexMap);
			}
		}
		return indexMap;
	}

	private static HashMap<String, metricsData> countDetails(ASTNode ast, HashMap<String, metricsData> freqRecord) {
		class NOACounter extends org.eclipse.wst.jsdt.core.dom.ASTVisitor {
			@Override
			public boolean visit(org.eclipse.wst.jsdt.core.dom.TypeDeclaration node) {
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

				FunctionDeclaration[] methods = node.getMethods();
				for(FunctionDeclaration declaration: methods){
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
