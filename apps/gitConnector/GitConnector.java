package gitConnector;

import br.ufpe.cin.groundhog.Issue;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nmtiwari on 7/9/16.
 */
public class GitConnector {
    private static String[] fixingPatterns = {"\\bfix(s|es|ing|ed)?\\b", "\\b(error|bug|issue)(s)?\\b"};
    private FileRepositoryBuilder builder;
    private Repository repository;
    private Git git;
//    private String userName;
//    private String projName;

    public GitConnector(String path) {
        this.builder = new FileRepositoryBuilder();
        try {
            this.repository = this.builder.setGitDir(new File(path + "/.git")).setMustExist(true).build();
            this.git = new Git(this.repository);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean cloneRepo(String URL, String repoPath) {
//        String url = URL.substring(URL.indexOf('@') + 1, URL.length()) + ".git";
        try {
            Github.clone(URL, repoPath);
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isFixingRevision(String commitLog) {
        boolean isFixing = false;
        Pattern p;
        if (commitLog != null) {
            String tmpLog = commitLog.toLowerCase();
            for (int i = 0; i < fixingPatterns.length; i++) {
                String patternStr = fixingPatterns[i];
                p = Pattern.compile(patternStr);
                Matcher m = p.matcher(tmpLog);
                isFixing = m.find();
                if (isFixing) {
                    break;
                }
            }
        }
        return isFixing;
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
        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(fileContent.toCharArray());
        parser.setCompilerOptions(options);
        ASTNode ast = parser.createAST(null);
        return ast;
    }

    public List<DiffEntry> diffsBetweenTwoRevAndChangeTypes(RevCommit cur, RevCommit prev)
            throws RevisionSyntaxException,
            IOException, GitAPIException {
        List<DiffEntry> diffs = new ArrayList<>();
      ObjectReader reader = this.repository.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, prev.getTree());
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, cur.getTree());

            // finally get the list of changed files
            diffs = this.git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
        return diffs;
    }

    /*
     * Reads a file from a commit id, if the file exists. Else throws
             * IllegalStateException
     */
    public String readFile(ObjectId lastCommitId, String path)
            throws IOException {
        // System.out.println("reading: " + path);
        RevWalk revWalk = new RevWalk(this.repository);
        RevCommit commit = revWalk.parseCommit(lastCommitId);
        // and using commit's tree find the path
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(this.repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(path));
        if (!treeWalk.next()) {
            throw new IllegalStateException(path);
        }
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = this.repository.open(objectId);
        InputStream in = loader.openStream();
        java.util.Scanner s = new java.util.Scanner(in); 
        s.useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        s.close();
        in.close();
        return result;
    }

    public List<Issue> getIssues(String username, String projName) {
        Github git = new Github(username, projName);
        List<Issue> issues = git.get_Issues();
        return issues;
    }

    public List<Integer> getIdsFromCommitMsg(String commitLog) {
        String commitMsg = commitLog;
        commitMsg = commitMsg.replaceAll("[^0-9]+", " ");
        List<String> idAsString = Arrays.asList(commitMsg.trim().split(" "));
        List<Integer> ids = new ArrayList<Integer>();
        for (String id : idAsString) {
            try{
                if(!ids.contains(Integer.parseInt(id)))
                    ids.add(Integer.parseInt(id));
            }catch(NumberFormatException e){
//                e.printStackTrace();
            }
        }
        return ids;
    }

    public List<Integer> getIssueIds(String username, String projName) {
        Github git = new Github(username, projName);
        List<Issue> issues = git.get_Issues();
        List<Integer> ids = new ArrayList<Integer>();
        for (Issue issue : issues) {
            ids.add(issue.getId());
        }
        return ids;
    }

    public List<Integer> getIssueIds(List<Issue> issues) {
        List<Integer> ids = new ArrayList<Integer>();
        for (Issue issue : issues) {
            ids.add(issue.getId());
        }
        return ids;
    }

    public boolean isFixingRevision(String msg, List<Issue> issues) {
        if (isFixingRevision(msg)) {
            List<Integer> ids = getIssueIds(issues);
            List<Integer> bugs = getIdsFromCommitMsg(msg);
            for (Integer i : bugs) {
                if (ids.contains(i)) {
                    return true;
                }
            }
        }
        return false;
    }
}
