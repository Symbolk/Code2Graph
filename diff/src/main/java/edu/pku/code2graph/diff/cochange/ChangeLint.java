package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.FileType;
import edu.pku.code2graph.diff.util.MetricUtil;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.SysUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.PropertyConfigurator;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class ChangeLint {
  static Logger logger = LoggerFactory.getLogger(ChangeLint.class);

  private static String repoName = "LeafPic";
  //  private static String repoName = "test_repo";
  private static String repoPath = "/Users/symbolk/coding/data/repos/" + repoName;
  private static String tempDir = "/Users/symbolk/coding/data/temp/c2g";

  static {
    initGenerators();
  }

  public static void initGenerators() {
    ClassIndex.getSubclasses(Generator.class)
        .forEach(
            gen -> {
              Register a = gen.getAnnotation(Register.class);
              if (a != null) Generators.getInstance().install(gen, a);
            });
  }

  public static void main(String[] args) throws IOException {

    //    BasicConfigurator.configure();
    PropertyConfigurator.configure(
        System.getProperty("user.dir") + File.separator + "log4j.properties");

    // 1. Offline process: given the commit id of the earliest future multi-lang commit
    // checkout to that version
    //    SysUtil.runSystemCommand(repoPath, Charset.defaultCharset(), "git", "checkout", "-b",
    // "changelint", "e457da8");
    //   build the graph for the current version
    Graph<Node, Edge> graph = buildGraph();
    logger.info(
        "Graph building done, with {} nodes and {} edges",
        graph.vertexSet().size(),
        graph.edgeSet().size());

    // 2. Online process: for each of the future commits, extract the changes as GT
    String testCommitID = "ea5ccf3";
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(testCommitID);
    //    DataCollector dataCollector = new DataCollector(tempDir);
    //    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);

    // Input: XMLDiff (file relative path, changed xml element id)
    Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
    // Ground Truth: JavaDiff (file relative path, type name, member name)
    Map<String, Set<Pair<String, String>>> javaDiffs = new HashMap<>();

    for (DiffFile diffFile : diffFiles) {
      if (diffFile.getFileType().equals(FileType.XML)) {
        xmlDiffs.put(
            diffFile.getARelativePath(),
            XMLDiffUtil.computeXMLChangesWithGumtree(
                diffFile.getAContent(), diffFile.getBContent()));
        //        xmlDiffs.put(
        //            diffFile.getARelativePath(),
        //                XMLDiffUtil.computeXMLChanges(
        //                tempFilePaths, diffFile.getARelativePath(), diffFile.getBRelativePath()));
      } else if (diffFile.getFileType().equals(FileType.JAVA)) {
        Set<Pair<String, String>> changes = JavaDiffUtil.computeJavaChanges(diffFile);
        if (!changes.isEmpty()) {
          // ignore files with only format/comment changes
          javaDiffs.put(diffFile.getARelativePath(), changes);
        }
      }
    }

    Set<String> xmlDiffElements = new HashSet<>();
    for (Map.Entry<String, List<XMLDiff>> entry : xmlDiffs.entrySet()) {
      for (XMLDiff diff : entry.getValue()) {
        if (XMLDiffUtil.isIDLabel(diff.getName())) {
          xmlDiffElements.add(diff.getName().replace("\"", "").replace("+", ""));
        }
      }
    }

    // Output: predicted co-change file/type/member in the graph
    Map<String, Set<Pair<String, String>>> javaCochanges = new HashMap<>();

    // check if exists to determine change action (if the tool reports wrong change action)
    Set<ElementNode> XMLNodes =
        graph.vertexSet().stream()
            .filter(v -> v.getLanguage().equals(Language.XML))
            .filter(v -> v instanceof ElementNode)
            .map(ElementNode.class::cast)
            .collect(Collectors.toSet());
    for (String element : xmlDiffElements) {
      Optional<ElementNode> nodeOpt =
          XMLNodes.stream().filter(node -> element.equals(node.getQualifiedName())).findAny();
      if (nodeOpt.isPresent()) {
        // locate changed xml nodes in the graph
        // if exist, modified/removed
        Set<Edge> useEdges =
            graph.incomingEdgesOf(nodeOpt.get()).stream()
                .filter(edge -> !edge.getType().equals(EdgeType.CHILD))
                .collect(Collectors.toSet());

        // find edge source (uses) nodes in Java code
        for (Edge useEdge : useEdges) {
          Node sourceNode = graph.getEdgeSource(useEdge);
          if (sourceNode.getLanguage().equals(Language.JAVA)) {
            // find wrapped member, type, and file nodes, return names
            addOutputEntry(javaCochanges, findWrappedEntities(graph, sourceNode));
          }
        }
      } else {
        // if not exist, added: predict co-changes according to context/similar nodes

      }
    }

    // measure accuracy by comparing with ground truth (compute the three sets)
    evaluate(javaCochanges, javaDiffs);
  }

  /**
   * TODO evaluate top-k precision
   *
   * @param output
   * @param groundTruth
   */
  private static void evaluate(
      Map<String, Set<Pair<String, String>>> output,
      Map<String, Set<Pair<String, String>>> groundTruth) {
    // separately on three levels
    int correctFileNum = 0;
    int correctTypeNum = 0;
    int correctMemberNum = 0;

    int otTotalFileNum = output.entrySet().size();
    int otTotalTypeNum = 0;
    int otTotalMemberNum = 0;
    for (Map.Entry<String, Set<Pair<String, String>>> entry : output.entrySet()) {
      String filePath = FileUtil.getRelativePath(repoPath, entry.getKey());
      Set<String> outputTypes = new HashSet<>();
      Set<String> outputMembers = new HashSet<>();
      entry
          .getValue()
          .forEach(
              pair -> {
                outputTypes.add(pair.getLeft());
                outputMembers.add(pair.getRight());
              });
      otTotalTypeNum += outputTypes.size();
      otTotalMemberNum += outputMembers.size();

      if (groundTruth.containsKey(filePath)) {
        Set<Pair<String, String>> gtTypesMembers = groundTruth.get(filePath);
        correctFileNum += 1;
        Set<String> gtTypes = new HashSet<>();
        Set<String> gtMembers = new HashSet<>();
        gtTypesMembers.forEach(
            pair -> {
              gtTypes.add(pair.getLeft());
              gtMembers.add(pair.getRight());
            });
        correctTypeNum += MetricUtil.intersectSize(outputTypes, gtTypes);
        correctMemberNum += MetricUtil.intersectSize(outputMembers, gtMembers);
      }
    }

    int gtAllFileNum = groundTruth.entrySet().size();
    Set<String> gtAllTypes = new HashSet<>();
    Set<String> gtAllMembers = new HashSet<>();
    for (Map.Entry<String, Set<Pair<String, String>>> entry : groundTruth.entrySet()) {
      for (Pair<String, String> pair : entry.getValue()) {
        if (!pair.getLeft().isEmpty()) {
          gtAllTypes.add(pair.getLeft());
        }

        if (!pair.getRight().isEmpty()) {
          gtAllMembers.add(pair.getRight());
        }
      }
    }

    // precision
    System.out.println(MetricUtil.formatDouble((double) correctFileNum / otTotalFileNum));
    System.out.println(MetricUtil.formatDouble((double) (correctTypeNum / otTotalTypeNum)));
    System.out.println(MetricUtil.formatDouble(((double) correctMemberNum / otTotalMemberNum)));

    // recall
    System.out.println(MetricUtil.formatDouble((double) correctFileNum / gtAllFileNum));
    System.out.println(MetricUtil.formatDouble((double) (correctTypeNum / gtAllTypes.size())));
    System.out.println(MetricUtil.formatDouble(((double) correctMemberNum / gtAllMembers.size())));
  }

  private static void addOutputEntry(
      Map<String, Set<Pair<String, String>>> javaCochanges,
      Triple<String, String, String> entityNames) {
    String filePath = entityNames.getLeft(),
        typeName = entityNames.getMiddle(),
        memberName = entityNames.getRight();

    if (!javaCochanges.containsKey(filePath)) {
      javaCochanges.put(filePath, new HashSet<>());
    }
    javaCochanges.get(filePath).add(Pair.of(typeName, memberName));
  }

  /**
   * (file relative path, type name, member name)
   *
   * @param node
   * @return
   */
  private static Triple<String, String, String> findWrappedEntities(
      Graph<Node, Edge> graph, Node node) {
    // iteratively find wrapped entities (until file)
    Optional<Node> parentOpt = Optional.of(node);
    String filePath = "", typeName = "", memberName = "";

    while (parentOpt.isPresent()) {
      Node parent = parentOpt.get();
      if (parent instanceof ElementNode) {
        Type type = parent.getType();
        if (NodeType.FILE.equals(type)) {
          filePath = ((ElementNode) parent).getQualifiedName();
        } else if (NodeType.ENUM_DECLARATION.equals(type)
            || NodeType.INTERFACE_DECLARATION.equals(type)
            || NodeType.CLASS_DECLARATION.equals(type)) {
          typeName = ((ElementNode) parent).getQualifiedName();
        } else if (parent.getType().isEntity) {
          memberName = ((ElementNode) parent).getName();
        }
      }
      parentOpt = getParentNode(graph, parent);
    }
    return Triple.of(filePath, typeName, memberName);
  }

  // TODO use git log to find co-changed elements from the past commits (sorted by confidence)

  /**
   * TODO: cache parent and children in Node, to speed up
   *
   * @param graph
   * @param node
   * @return
   */
  private static Optional<Node> getParentNode(Graph<Node, Edge> graph, Node node) {
    return graph.incomingEdgesOf(node).stream()
        .filter(edge -> edge.getType().equals(EdgeType.CHILD))
        .map(graph::getEdgeSource)
        .findFirst();
  }

  private static Graph<Node, Edge> buildGraph() throws IOException {
    // iterate all Java files and match imports
    List<String> allJavaFilePaths = FileUtil.getSpecificFilePaths(repoPath, ".java");

    Map<String, List<String>> rJavaPathsAndImports = new HashMap<>();
    for (String path : allJavaFilePaths) {
      List<String> lines = FileUtil.readFileToLines(path);
      String packageName = "";
      for (String line : lines) {
        line = line.trim();
        if (line.startsWith("public class")) {
          break;
        }

        if (line.startsWith("package")) {
          packageName = line.replaceFirst("package", "").trim();
          continue;
        }

        // if imports R, collect other imported project source files
        if (line.startsWith("import") && line.endsWith(".R;") && !packageName.isEmpty()) {
          String rPackageName = line.replaceFirst("import", "").replace(".R;", "").trim();
          String commonPrefix = StringUtils.getCommonPrefix(packageName, rPackageName);
          List<String> imports =
              lines.stream()
                  .map(String::trim)
                  .filter(
                      l -> l.startsWith("import") && l.contains(commonPrefix) && !l.endsWith(".R;"))
                  .collect(Collectors.toList());

          // root folder that the package is related to
          String rootSrcFolder = path.substring(0, path.indexOf(convertQNameToPath(packageName)));
          // get absolute paths of imported files
          List<String> importedJavaPaths = new ArrayList<>();
          for (String im : imports) {
            importedJavaPaths.add(rootSrcFolder + convertQNameToPath(im) + ".java");
          }
          rJavaPathsAndImports.put(path, importedJavaPaths);
        }
      }
    }

    Set<String> filePaths = new HashSet<>();
    for (Map.Entry<String, List<String>> entry : rJavaPathsAndImports.entrySet()) {
      filePaths.add(entry.getKey());
      filePaths.addAll(entry.getValue());
    }

    // collect all xml files (filter only layout?)
    filePaths.addAll(
        FileUtil.getSpecificFilePaths(repoPath, ".xml").stream()
            .filter(path -> path.contains("layout"))
            .collect(Collectors.toList()));

    Generators generator = Generators.getInstance();

    return generator.generateFromFiles(new ArrayList<>(filePaths));
  }

  /** Convert package and import statement to path */
  private static String convertQNameToPath(String importStatement) {
    return importStatement
        .trim()
        .replace("import ", "") // just in case
        .replace("package ", "")
        .replace(";", "")
        .replace(";", "")
        .replace(".", File.separator)
        .trim();
  }
}
