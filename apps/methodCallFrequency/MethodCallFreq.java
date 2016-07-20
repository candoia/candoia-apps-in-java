package methodCallFrequency;

import gitConnector.GitConnector;
import org.eclipse.jdt.core.dom.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nmtiwari on 7/20/16.
 */
public class MethodCallFreq {
    private GitConnector git;
    private String userName;
    private String projName;

    private MethodCallFreq(String repoPath) {
        this.git = new GitConnector(repoPath);
        String[] details = repoPath.split("/");
        this.projName = details[details.length - 1];
        this.userName = details[details.length - 2];
    }

    /*
     * url must be of form: username@url
     */
    private MethodCallFreq(String url, String path) {
        this.userName = url.substring(0, url.indexOf('@'));
        url = url.substring(url.indexOf('@') + 1);
        this.projName = url.substring(url.lastIndexOf('/') + 1);
        GitConnector.cloneRepo(url, path);
        this.git = new GitConnector(path);
    }

    /*
     * Main function for NullCheckGit
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        MethodCallFreq freq = null;
        // path of the repository
        if (args.length < 1) {
            freq = new MethodCallFreq("/Users/nmtiwari/git/research/candoia/candoia-apps-in-java");
        } else if (args.length == 2) {
            freq = new MethodCallFreq(args[1], args[0]);
        } else {
            freq = new MethodCallFreq(args[0]);
        }

        List<String> allFiles = freq.git.getAllFilesFromHeadWithAbsPath();
        HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
        for (String path : allFiles) {
            if (path.endsWith(".java")) {
                String content = freq.readFile(path);
                ASTNode ast = freq.git.createAst(content);
                indexMap = countMethodCallFreq(ast, indexMap);
            }
        }

        for (String str : indexMap.keySet()) {
            System.out.println(str + " -> " + indexMap.get(str));
        }
    }

    private static HashMap<String, Integer> countMethodCallFreq(ASTNode ast, HashMap<String, Integer> map) {
        HashMap<String, Integer> freqRecord = map;
        class MethodCallFreqVisitor extends ASTVisitor {
            @Override
            public boolean visit(MethodDeclaration node) {
                String methodName = node.getName().getFullyQualifiedName().toString();
//                    String entry = className + "->" + methodName;
                String entry = methodName;
                if (!freqRecord.containsKey(entry)) {
                    freqRecord.put(entry, 0);
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node) {
                String mName = node.getName().getFullyQualifiedName().toString();
                if (freqRecord.containsKey(mName)) {
                    freqRecord.put(mName, freqRecord.get(mName) + 1);
                } else {
                    freqRecord.put(mName, 1);
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


            class ClassDeclarationVisitor extends ASTVisitor {
                String className = "";

                @Override
                public boolean visit(TypeDeclaration node) {
                    className = node.getName().getFullyQualifiedName().toString();
                    return super.visit(node);
                }

                @Override
                public boolean visit(MethodDeclaration node) {
                    String methodName = node.getName().getFullyQualifiedName().toString();
//                    String entry = className + "->" + methodName;
                    String entry = methodName;
                    if (!freqRecord.containsKey(entry)) {
                        freqRecord.put(entry, 0);
                    }
                    return super.visit(node);
                }

                class MethodFreqExpressionVisitor extends ASTVisitor {
                    @Override
                    public boolean visit(MethodInvocation node) {
                        String mName = node.getName().getFullyQualifiedName().toString();
                        if (freqRecord.containsKey(mName)) {
                            freqRecord.put(mName, freqRecord.get(mName) + 1);
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
            System.out.println(
                    "Unable to open file '" +
                            path + "'");
        } catch (IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + path + "'");
            // Or we could just do this:
            // ex.printStackTrace();
        }
        return content;
    }

}
