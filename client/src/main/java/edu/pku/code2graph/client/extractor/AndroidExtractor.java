package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.gen.jdt.AbstractJdtVisitor;
import edu.pku.code2graph.gen.xml.SaxGenerator;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
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

public class AndroidExtractor extends AbstractExtractor {
  public List<URI> xmlURIS = new ArrayList<>();
  public Map<String, List<URI>> javaURIS = new HashMap<>();

  private static final String JRE_PATH =
      System.getProperty("java.home") + File.separator + "lib/rt.jar";

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

    Map<String, List<String>> layMap = new HashMap<>();
    Map<String, List<URI>> idMap = new HashMap<>();

    AbstractJdtVisitor visitor = new AndroidExpressionVisitor(layMap, idMap);
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

    for (String fileName : filePaths) {
      List<String> layouts = layMap.get(fileName);
      List<URI> ids = idMap.get(fileName);
      if (ids == null || ids.isEmpty()) continue;
      if (layouts == null || layouts.isEmpty()) {
        if (!javaURIS.containsKey("")) javaURIS.put("", new ArrayList<>());
        for (URI id : ids) {
          javaURIS.get("").add(id);
        }
      } else {
        for (String layout : layouts) {
          if (!javaURIS.containsKey(layout)) javaURIS.put(layout, new ArrayList<>());
          for (URI id : ids) {
            javaURIS.get(layout).add(id);
          }
        }
      }
    }

    for (String layout : javaURIS.keySet()) {
      List<URI> uris = removeDuplicateOutputField(javaURIS.get(layout));
      javaURIS.get(layout).clear();
      javaURIS.put(layout, uris);
    }

    GraphUtil.clearGraph();
  }

  public void extractXmlUri(String repoPath) throws IOException {
    GraphUtil.clearGraph();

    List<String> filePaths = new ArrayList<>();
    List<String> exts = Arrays.asList(".xml");
    findExtInRepo(repoPath, exts, filePaths);

    SaxGenerator generator = new SaxGenerator();
    Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
    for (Node node : graph.vertexSet()) {
      if (node instanceof ElementNode
          && node.getUri().getInline() != null
          && node.getUri().getInline().getIdentifier().startsWith("@+id")) {
        xmlURIS.add(node.getUri());
      }
    }

    xmlURIS = removeDuplicateOutputField(xmlURIS);

    GraphUtil.clearGraph();
  }

  private void findPairByXmlUri(URI uri) {
    String layout = uri.getFile();
    String[] split = layout.split("/");
    layout = "R.layout." + split[split.length - 1];
    layout = layout.substring(0, layout.length() - 4);
    if(javaURIS.containsKey("")){
      for (URI item : javaURIS.get("")) {
        if (item.getSymbol().length() >= 5 && item.getSymbol().substring(5).equals(uri.getSymbol()))
          uriPairs.add(new ImmutablePair<>(uri, item));
      }
    }

    if (!javaURIS.containsKey(layout)) return;
    for (URI item : javaURIS.get(layout)) {
      if (item.getSymbol().length() >= 5
          && item.getSymbol().substring(5).equals(uri.getSymbol()))
        uriPairs.add(new ImmutablePair<>(uri, item));
    }
  }

  private void findPairs() {
    for (URI uri : xmlURIS) {
      findPairByXmlUri(uri);
    }
  }

  public List<Pair<URI, URI>> generateInstances(String repoRoot, String repoPath)
      throws IOException {
    FileUtil.setRootPath(repoRoot);
    extractJavaUri(repoPath);
    extractXmlUri(repoPath);
    findPairs();
    return uriPairs;
  }
}
