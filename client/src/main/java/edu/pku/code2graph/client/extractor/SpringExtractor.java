package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.gen.html.JsoupGenerator;
import edu.pku.code2graph.gen.html.model.NodeType;
import edu.pku.code2graph.gen.jdt.AbstractJdtVisitor;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class SpringExtractor extends AbstractExtractor {
  public List<URI> htmlURIS = new ArrayList<>();
  public Map<String, List<URI>> javaURIS = new HashMap<>();
  public Map<String, List<URI>> viewPathReturns = new HashMap<>();

  private static final String JRE_PATH =
      System.getProperty("java.home") + File.separator + "lib/rt.jar";

  public void extractHtmlUri(String repoPath) throws IOException {
    GraphUtil.clearGraph();

    List<String> filePaths = new ArrayList<>();
    List<String> exts = Arrays.asList(".html", ".jsp");
    findExtInRepo(repoPath, exts, filePaths);

    JsoupGenerator generator = new JsoupGenerator();
    Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
    URITree tree = GraphUtil.getUriTree();
    for (Node node : tree.getAllNodes()) {
      if (node instanceof ElementNode
          && (node.getType().equals(NodeType.INLINE_VAR) || node.getType().equals(NodeType.FILE))) {
        System.out.println(((ElementNode) node).getName());
        htmlURIS.add(node.getUri());
      }
    }

    htmlURIS = removeDuplicateOutputField(htmlURIS);
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

    AbstractJdtVisitor visitor = new SpringExpressionVisitor(javaURIS, viewPathReturns);
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

  private void findPairByHtmlUri(URI uri) {
    String filename = uri.getFile();
    String[] split = filename.split("\\.");
    String fileWithoutExt = split[split.length - 2];
    if (uri.getLayerCount() == 1) {
      for (String key : viewPathReturns.keySet()) {
        String[] filePathSplit = fileWithoutExt.split("/");
        String fileNameWithoutExt = filePathSplit[filePathSplit.length - 1];
        filePathSplit = filename.split("/");
        String fileName = filePathSplit[filePathSplit.length - 1];
        if (key.contains("/") && (fileWithoutExt.endsWith(key) || filename.endsWith(key))
            || fileNameWithoutExt.equals(key)
            || fileName.equals(key)) {
          for (URI val : viewPathReturns.get(key)) {
            int pos = filename.indexOf("src/main");
            if (pos == -1) continue;
            String prefix = filename.substring(0, pos);
            if (val.getLayer(0).get("identifier").startsWith(prefix))
              uriPairs.add(new ImmutablePair<>(val, uri));
          }
        }
      }
      return;
    }

    String sym = uri.getSymbol();
    sym = sym.substring(2, sym.length() - 1).trim();
    if (sym.matches("[a-zA-Z_.0-9]+") && sym.contains(".")) {
      sym = sym.split("\\.")[0];
    }
    for (String key : javaURIS.keySet()) {
      String[] filePathSplit = fileWithoutExt.split("/");
      String fileNameWithoutExt = filePathSplit[filePathSplit.length - 1];
      filePathSplit = filename.split("/");
      String fileName = filePathSplit[filePathSplit.length - 1];
      if (key.contains("/") && (fileWithoutExt.endsWith(key) || filename.endsWith(key))
          || fileNameWithoutExt.equals(key)
          || fileName.equals(key)) {
        for (URI val : javaURIS.get(key)) {
          if (val.getSymbol().equals(sym)) {
            int pos = filename.indexOf("src/main");
            if (pos == -1) continue;
            String prefix = filename.substring(0, pos);
            if (val.getLayer(0).get("identifier").startsWith(prefix))
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
}
