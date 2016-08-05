package methodCallFrequency;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.tmatesoft.svn.core.SVNProperties;

import svnConnector.SVNConnector;

/**
 * Created by nmtiwari on 7/20/16.
 */
public class MethodUsageFrequencySVN {
	private SVNConnector svn;
	private String userName;
	private String projName;

	private MethodUsageFrequencySVN(String repoPath) {
		this.svn = new SVNConnector(repoPath);
		String[] details = repoPath.split("/");
		this.projName = details[details.length - 1];
		this.userName = details[details.length - 2];
	}

	/*
	 * url must be of form: username@url
	 */
	private MethodUsageFrequencySVN(String url, String path) {
		this.userName = url.substring(0, url.indexOf('@'));
		url = url.substring(url.indexOf('@') + 1);
		this.projName = url.substring(url.lastIndexOf('/') + 1);
		SVNConnector.cloneRepo(url, path);
		this.svn = new SVNConnector(path);
	}

	/*
	 * Main function for NullCheckGit
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();

		HashMap<String, Integer> indexMap = MethodUsageFrequencySVN.analyze(args);
		for (String str : indexMap.keySet()) {
			System.out.println(str + " -> " + indexMap.get(str));
		}
		HashMap<String, Integer> updaedIndexMap = new HashMap<>();
		for (String str : indexMap.keySet()) {
			if(indexMap.get(str) > 15){
				updaedIndexMap.put(str.replaceAll("\n", ""), indexMap.get(str));	
			}
			
		}
		MethodUsageCharting.saveGraph(updaedIndexMap, "/Users/nmtiwari/Desktop/graph.html");
	}

	/*
	 * Main function for NullCheckGit
	 */
	public static HashMap<String, Integer> analyze(String[] args) {
		long startTime = System.currentTimeMillis();
		MethodUsageFrequencySVN freq = null;
		// path of the repository
		if (args.length < 1) {
			freq = new MethodUsageFrequencySVN("/Users/nmtiwari/Desktop/test/pagal/projects");
		} else if (args.length == 2) {
			freq = new MethodUsageFrequencySVN(args[1], args[0]);
		} else {
			freq = new MethodUsageFrequencySVN(args[0]);
		}

		ArrayList<String> allFiles = new ArrayList<>();
		allFiles = freq.svn.getAllFilesFromHead(allFiles);
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
		HashMap<String, String> varTyp = new HashMap<String, String>();

		for (String path : allFiles) {
			if (path.endsWith(".java")) {
				// String content = freq.readFile(path);
				String content = freq.svn.getFileContent(path, -1, new SVNProperties(), new ByteArrayOutputStream());
				ASTNode ast = freq.svn.createAst(content);
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

	private static HashMap<String, Integer> countMethodCallFrequency(ASTNode ast, HashMap<String, Integer> freqRecord,
			HashMap<String, String> varTypMap) {
		// HashMap<String, Integer> freqRecord = map;
		class MethodCallFreqVisitor extends ASTVisitor {
			@Override
			public boolean visit(MethodDeclaration node) {
				String methodName = node.getName().getFullyQualifiedName().toString();
				String entry = methodName;
				if (!freqRecord.containsKey(entry)) {
					freqRecord.put(entry, 0);
				}
				return super.visit(node);
			}

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
				}
				if (varTypMap.containsKey(mName)) {
					typName = varTypMap.get(mName);
				}
				if (freqRecord.containsKey(mName)) {
					freqRecord.put(typName + "->" + mName, freqRecord.get(mName) + 1);
				} else {
					freqRecord.put(typName + "->" + mName, 1);
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(SuperMethodInvocation node) {
				String mName = node.getName().getFullyQualifiedName().toString();
				if (freqRecord.containsKey(mName)) {
					freqRecord.put(mName, freqRecord.get(mName) + 1);
				}
				return true;
			}
		}
		MethodCallFreqVisitor v = new MethodCallFreqVisitor();
		ast.accept(v);
		return freqRecord;
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

	private String readFile(String path) {
		String line = null;
		String content = "";
		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(path);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				content += line;
			}

			// Always close files.
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + path + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + path + "'");
			// Or we could just do this:
			// ex.printStackTrace();
		}
		return content;
	}

}
