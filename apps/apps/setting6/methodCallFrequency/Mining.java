package setting6.methodCallFrequency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;

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
			if (indexMap.get(str) > 10) {
				results.put(str, indexMap.get(str));
			}

		}
		Visualization.saveGraph(results, args[1] + "_methodCallFrequency.html");
	}

	public HashMap<String, Integer> analyze() {
		List<String> allFiles = this.git.getAllFilesFromHeadWithAbsPath();
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
		HashMap<String, String> varTyp = new HashMap<String, String>();

		for (String path : allFiles) {
			if (path.endsWith(".js")) {
				String content = this.readFile(path);
				ASTNode ast = this.git.createAst(content);
				varTyp = countMethodCallFreqWithTypes(ast, varTyp);
				indexMap = countMethodCallFreq(ast, indexMap, varTyp);
			}
		}
		return indexMap;
	}

	private static HashMap<String, String> countMethodCallFreqWithTypes(ASTNode ast, HashMap<String, String> mapper) {
		HashMap<String, String> map = mapper;
		class TypeDeclaratorMapper extends ASTVisitor {
			@Override
			public boolean visit(VariableDeclarationStatement node) {
				Type typ = node.getType();
				if (typ.isSimpleType()) {
					SimpleType simple = (SimpleType) typ;
					String typName = simple.getName().getFullyQualifiedName();
					List<VariableDeclarationFragment> vars = node.fragments();
					for (VariableDeclarationFragment var : vars) {
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				if (typ.isQualifiedType()) {
					QualifiedType qual = (QualifiedType) typ;
					String typName = qual.getName().getFullyQualifiedName();
					List<VariableDeclarationFragment> vars = node.fragments();
					for (VariableDeclarationFragment var : vars) {
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				if (typ.isPrimitiveType()) {
					PrimitiveType prim = (PrimitiveType) typ;
					String typName = prim.getPrimitiveTypeCode().toString();
					List<VariableDeclarationFragment> vars = node.fragments();
					for (VariableDeclarationFragment var : vars) {
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				if (typ.isArrayType()) {
					ArrayType prim = (ArrayType) typ;
					String typName = "Array";
					List<VariableDeclarationFragment> vars = node.fragments();
					for (VariableDeclarationFragment var : vars) {
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				return true;
			}

			@Override
			public boolean visit(FunctionDeclaration node) {
				for (Object param : node.parameters()) {
					SingleVariableDeclaration arg = (SingleVariableDeclaration) param;
					String name = arg.getName().getFullyQualifiedName();
					String typName = arg.getType().toString();
					map.put(name, typName);
				}
				return true;
			}

			@Override
			public boolean visit(FieldDeclaration node) {
				String typName = node.getType().toString();
				List<VariableDeclarationFragment> vars = node.fragments();
				for (VariableDeclarationFragment var : vars) {
					map.put(var.getName().getIdentifier(), typName);
				}
				return true;
			}
		}

		TypeDeclaratorMapper v = new TypeDeclaratorMapper();
		ast.accept(v);
		return map;
	}

	private static HashMap<String, Integer> countMethodCallFreq(ASTNode ast, HashMap<String, Integer> freqRecord,
			HashMap<String, String> varTypMap) {
		class MethodCallFreqVisitor extends ASTVisitor {

			@Override
			public boolean visit(FunctionInvocation node) {
				String mName = node.getName().getFullyQualifiedName().toString();
				Expression e = node.getExpression();
				String typName = "";
				if (e instanceof StringLiteral) {
					typName = "string";
				} else if (e instanceof FieldAccess) {
					FieldAccess field = (FieldAccess) e;
					typName = field.getName().getFullyQualifiedName();
				} else if (e instanceof Name) {
					typName = ((Name) e).getFullyQualifiedName();
					if (varTypMap.containsKey(typName)) {
						typName = varTypMap.get(typName);
					}
				} else {
					if (e != null) {
						typName = e.toString();
						if (typName.contains("."))
							typName = typName.substring(0, typName.indexOf('.', 0));
						if (varTypMap.containsKey(typName))
							typName = varTypMap.get(typName);
					}
				}

				String key = typName + "->" + mName;
				if (freqRecord.containsKey(key)) {
					freqRecord.put(key, freqRecord.get(key) + 1);
				} else {
					freqRecord.put(key, 1);
				}
				return super.visit(node);
			}
		}
		MethodCallFreqVisitor v = new MethodCallFreqVisitor();
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
