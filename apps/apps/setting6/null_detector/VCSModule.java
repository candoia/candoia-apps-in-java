package setting6.null_detector;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VCSModule {
	private Repository repository;
	private String path;

	public VCSModule(String path) {
		this.path = path;
		try {
			this.repository = new FileRepositoryBuilder().setGitDir(new File(path + "/.git")).setMustExist(true).build();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ASTNode createAst(String fileContent) {
		Map<String, String> options = JavaScriptCore.getOptions();
		options.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_5);
		options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_5);
		options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_5);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(fileContent.toCharArray());
		parser.setCompilerOptions(options);
		ASTNode ast = parser.createAST(null);
		return ast;
	}

	public List<String> getAllFilesFromHeadWithAbsPath() {
		ArrayList<String> results = new ArrayList<>();
		try {
			Ref head = this.repository.getRef("HEAD");
			RevWalk walk = new RevWalk(this.repository);
			RevCommit revision = walk.parseCommit(head.getObjectId());
			RevTree tree = revision.getTree();
			TreeWalk walker = new TreeWalk(this.repository);
			walker.addTree(tree);
			walker.setRecursive(true);
			while (walker.next()) {
				results.add(this.path + "/" + walker.getPathString());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return results;
	}
}
