package methodCallFrequency.setting6;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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
	private static String[] fixingPatterns = { "\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b" };
	private FileRepositoryBuilder builder;
	private Repository repository;
	private Git git;
	private String path;

	public VCSModule(String path) {
		this.builder = new FileRepositoryBuilder();
		this.path = path;
		try {
			this.repository = this.builder.setGitDir(new File(path + "/.git")).setMustExist(true).build();
			this.git = new Git(this.repository);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Repository getRepository() {
		return this.repository;
	}

	public ArrayList<RevCommit> getAllRevisions() {
		ArrayList<RevCommit> revisions = new ArrayList<>();

		Iterable<RevCommit> allRevisions = null;
		try {
			allRevisions = git.log().call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}

		for (RevCommit rev : allRevisions) {
			revisions.add(rev);
		}
		return revisions;
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
			walk.close();
			walker.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return results;
	}
}
