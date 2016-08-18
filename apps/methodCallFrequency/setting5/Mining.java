package methodCallFrequency.setting5;

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
						String identifier = var.getName().getIdentifier();
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				if (typ.isQualifiedType()) {
					QualifiedType qual = (QualifiedType) typ;
					String typName = qual.getName().getFullyQualifiedName();
					List<VariableDeclarationFragment> vars = node.fragments();
					for (VariableDeclarationFragment var : vars) {
						String identifier = var.getName().getIdentifier();
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				if (typ.isPrimitiveType()) {
					PrimitiveType prim = (PrimitiveType) typ;
					String typName = prim.getPrimitiveTypeCode().toString();
					List<VariableDeclarationFragment> vars = node.fragments();
					for (VariableDeclarationFragment var : vars) {
						String identifier = var.getName().getIdentifier();
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				if (typ.isParameterizedType()) {
					ParameterizedType prim = (ParameterizedType) typ;
					String typName = prim.getType().toString();
					List<VariableDeclarationFragment> vars = node.fragments();
					for (VariableDeclarationFragment var : vars) {
						String identifier = var.getName().getIdentifier();
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				if (typ.isArrayType()) {
					ArrayType prim = (ArrayType) typ;
					String typName = "Array";
					List<VariableDeclarationFragment> vars = node.fragments();
					for (VariableDeclarationFragment var : vars) {
						String identifier = var.getName().getIdentifier();
						map.put(var.getName().getIdentifier(), typName);
					}
				}
				return true;
			}

			@Override
			public boolean visit(MethodDeclaration node) {
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
					String identifier = var.getName().getIdentifier();
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
			public boolean visit(MethodInvocation node) {
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
						if (typName.contains(".")) {
							typName = typName.substring(0, typName.indexOf('.', 0));
						}
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
}
