package edu.pku.code2graph.client.extractor;

import com.csvreader.CsvWriter;
import edu.pku.code2graph.gen.html.JsoupGenerator;
import edu.pku.code2graph.gen.html.model.NodeType;
import edu.pku.code2graph.gen.jdt.AbstractJdtVisitor;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.jgrapht.Graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;

public class SpringExtractor {
  public List<URI> htmlURIS = new ArrayList<>();
  public Map<String, List<URI>> javaURIS = new HashMap<>();
  public List<Pair<URI, URI>> uriPairs = new ArrayList<>();

  private static final String JRE_PATH =
      System.getProperty("java.home") + File.separator + "lib/rt.jar";

  public void extractHtmlUri(String repoPath) throws IOException {
    GraphUtil.clearGraph();

    List<String> filePaths = new ArrayList<>();
    List<String> exts = Arrays.asList(".html", ".jsp");
    findExtInRepo(repoPath, exts, filePaths);

    JsoupGenerator generator = new JsoupGenerator();
    Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
    for (Node node : graph.vertexSet()) {
      if (node instanceof ElementNode
          && node.getType().equals(NodeType.INLINE_VAR)
          && !((ElementNode) node).getName().contains("$")) {
        System.out.println(((ElementNode) node).getName());
        htmlURIS.add(node.getUri());
      }
    }

    removeDuplicateOutputField(htmlURIS);
    GraphUtil.clearGraph();
  }

  public void extractJavaUri(String repoPath) {
    GraphUtil.clearGraph();

    List<String> filePaths = new ArrayList<>();
    List<String> exts = Arrays.asList(".java");
    findExtInRepo(repoPath, exts, filePaths);

    // the absolute file path of the compilation units to create ASTs for
    String[] srcPaths = new String[filePaths.size()];
    filePaths.toArray(srcPaths);

    // the absolute source path entries to be used to resolve bindings
    Set<String> srcFolderSet = new HashSet<>();
    for (String path : filePaths) {
      File file = new File(path);
      if (file.exists() && file.isFile()) {
        srcFolderSet.add(file.getParentFile().getAbsolutePath());
      }
    }
    String[] srcFolderPaths = new String[srcFolderSet.size()];
    srcFolderSet.toArray(srcFolderPaths);

    //
    String[] encodings = new String[srcFolderPaths.length];
    Arrays.fill(encodings, "UTF-8");

    ASTParser parser = ASTParser.newParser(AST.JLS14);
    Map<String, String> options = JavaCore.getOptions();
    options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
    options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
    parser.setCompilerOptions(options);

    //        parser.setProject(WorkspaceUtilities.javaProject);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setEnvironment(new String[] {JRE_PATH}, srcFolderPaths, encodings, true);
    parser.setResolveBindings(true);
    parser.setBindingsRecovery(true);

    AbstractJdtVisitor visitor = new ExpressionVisitor(javaURIS);
    // create nodes and nesting edges while visiting the ASTs
    encodings = new String[srcPaths.length];
    Arrays.fill(encodings, "UTF-8");

    parser.createASTs(
        srcPaths,
        encodings,
        new String[] {},
        new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit cu) {
            // use relative path to project root
            visitor.setFilePath(sourceFilePath);
            visitor.setCu(cu);
            cu.accept(visitor);
          }
        },
        null);

    for (String key : javaURIS.keySet()) {
      List<URI> uris = removeDuplicateOutputField(javaURIS.get(key));
      javaURIS.get(key).clear();
      javaURIS.put(key, uris);
    }
  }

  private static List<URI> removeDuplicateOutputField(List<URI> list) {
    Set<URI> set =
        new TreeSet<>(
            (a, b) -> {
              int compareToResult = 1; // ==0表示重复
              if (StringUtils.equals(a.toString(), b.toString())) {
                compareToResult = 0;
              }
              return compareToResult;
            });
    set.addAll(list);
    return new ArrayList<>(set);
  }

  private void findPairByHtmlUri(URI uri) {
    for (String key : javaURIS.keySet()) {
      if (uri.getFile().contains(key)) {
        for (URI val : javaURIS.get(key)) {
          if (val.getSymbol().equals(uri.getSymbol())) {
            uriPairs.add(new ImmutablePair<>(uri, val));
          }
        }
      }
    }
  }

  private void findPairs() {
    for (URI uri : htmlURIS) {
      findPairByHtmlUri(uri);
    }
  }

  public List<Pair<URI, URI>> generateInstances(String repoRoot, String repoPath)
      throws IOException {
    FileUtil.setRootPath(repoRoot);
    extractHtmlUri(repoPath);
    extractJavaUri(repoPath);
    findPairs();
    return uriPairs;
  }

  public void writeToFile(String filePath) throws IOException {
    File outFile = new File(filePath);
    if (!outFile.exists()) {
      outFile.createNewFile();
    }
    CsvWriter writer = new CsvWriter(filePath, ',', Charset.forName("UTF-8"));
    String[] headers = {"HTML", "JAVA"};
    writer.writeRecord(headers);
    for (Pair<URI, URI> pair : uriPairs) {
      String left = pair.getLeft().toString(), right = pair.getRight().toString();
      String[] record = {
        left.substring(5, left.length() - 1), right.substring(5, right.length() - 1)
      };
      writer.writeRecord(record);
    }
    writer.close();
  }

  private void findExtInRepo(String path, List<String> exts, List<String> filePaths) {
    File file = new File(path);
    LinkedList<File> list = new LinkedList<>();
    if (file.exists()) {
      if (null == file.listFiles()) {
        return;
      }
      list.addAll(Arrays.asList(file.listFiles()));
      while (!list.isEmpty()) {
        File[] files = list.removeFirst().listFiles();
        if (null == files) {
          continue;
        }
        for (File f : files) {
          if (f.isDirectory()) {
            list.add(f);
          } else {
            for (String ext : exts) {
              if (f.getName().length() > ext.length() && f.getName().endsWith(ext)) {
                filePaths.add(f.getAbsolutePath());
              }
            }
          }
        }
      }
    }
  }
}
