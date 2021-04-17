package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.ChangeType;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.PropertyConfigurator;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class ChangeLint {
  static Logger logger = LoggerFactory.getLogger(ChangeLint.class);

  private static String rootFolder = "/Users/symbolk/coding/data/changelint";
  private static String repoPath = rootFolder + "/repos";
  private static final String tempDir = rootFolder + "/temp";

  private static String commitsListPath = rootFolder + "/cross-lang-commits/eh";

  private static Map<String, Double> cochangeFiles = new HashMap<>();
  private static Map<String, Double> cochangeTypes = new HashMap<>();
  private static Map<String, Double> cochangeMembers = new HashMap<>();

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

  public static void main(String[] args) throws IOException, ParseException {

    //    BasicConfigurator.configure();
    PropertyConfigurator.configure(
        System.getProperty("user.dir") + File.separator + "log4j.properties");

    // read commit list and filter commit
    List<String> filePaths = FileUtil.listFilePaths(commitsListPath, "");
    //    String repoName = FileUtil.getFileNameFromPath(commitsListPath);
    String repoName = "EhViewer";
    repoPath = repoPath + File.separator + repoName;

    for (String filePath : filePaths) {
      JSONParser parser = new JSONParser();
      JSONArray commitList = (JSONArray) parser.parse(new FileReader(filePath));

      for (JSONObject commit : (Iterable<JSONObject>) commitList) {
        if (hasViewChanges(commit)) {
          //          String testCommitID = (String) commit.get("commit_id");
          String testCommitID = "ee0ffc4";
          // 1. Offline process: given the commit id of the earliest future multi-lang commit
          logger.info("Processing repo: {} at {}", repoName, repoPath);

          // checkout to the previous version
          logger.info("Checking out to previous commit of {}", testCommitID);
          SysUtil.runSystemCommand(
              repoPath,
              Charset.defaultCharset(),
              "git",
              "checkout", /* "-b","changelint", */
              testCommitID + "~");

          //   build the graph for the current version
          Graph<Node, Edge> graph = buildGraph();

          logger.info(
              "Graph building done, nodes: {}; edges: {}",
              graph.vertexSet().size(),
              graph.edgeSet().size());

          // 2. Online process: for each of the future commits, extract the changes as GT
          logger.info("Computing diffs for commit: {}", testCommitID);
          RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
          List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(testCommitID);

          //    DataCollector dataCollector = new DataCollector(tempDir);
          //    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);

          // Input: XMLDiff (file relative path: <file relative path, changed xml element type, id>)
          Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
          // Ground Truth: JavaDiff (file relative path: <file relative path, type name, member
          // name>)
          Map<String, List<JavaDiff>> javaDiffs = new HashMap<>();

          for (DiffFile diffFile : diffFiles) {
            if (diffFile.getFileType().equals(FileType.XML)
                && diffFile.getARelativePath().contains("layout")) {
              xmlDiffs.put(
                  diffFile.getARelativePath(), XMLDiffUtil.computeXMLChangesWithGumtree(diffFile));
              //        xmlDiffs.put(
              //            diffFile.getARelativePath(),
              //                XMLDiffUtil.computeXMLChanges(
              //                tempFilePaths, diffFile.getARelativePath(),
              // diffFile.getBRelativePath()));
            } else if (diffFile.getFileType().equals(FileType.JAVA)) {
              List<JavaDiff> changes = JavaDiffUtil.computeJavaChanges(diffFile);
              if (!changes.isEmpty()) {
                // ignore files with only format/comment changes
                javaDiffs.put(diffFile.getARelativePath(), changes);
              }
            }
          }

          logger.info("XML diff files: {}", xmlDiffs.entrySet().size());
          logger.info("Java diff files: {}", javaDiffs.entrySet().size());

          cochangeFiles = new HashMap<>();
          cochangeTypes = new HashMap<>();
          cochangeMembers = new HashMap<>();

          // Output: predicted co-change file/type/member in the graph
          //    Map<String, List<JavaDiff>> suggestedCoChanges = new HashMap<>();
          // check if exists to determine change action (if the tool reports wrong change action)
          Set<ElementNode> XMLNodes =
              graph.vertexSet().stream()
                  .filter(v -> v.getLanguage().equals(Language.XML))
                  .filter(v -> v instanceof ElementNode)
                  .map(ElementNode.class::cast)
                  .collect(Collectors.toSet());
          // for xml changes in each xml file
          for (Map.Entry<String, List<XMLDiff>> entry : xmlDiffs.entrySet()) {
            String path = entry.getKey();
            String viewID =
                "@"
                    + FileUtil.getParentFolderName(path)
                    + "/"
                    + FilenameUtils.removeExtension(FileUtil.getFileNameFromPath(path));
            // the node representing the view file
            ElementNode viewNode =
                XMLNodes.stream()
                    .filter(node -> viewID.equals(node.getQualifiedName()))
                    .findFirst()
                    .get();
            // find the file name that directly/indirectly refs the view node
            Set<Edge> useEdges = getUseEdges(graph, viewNode);
            Set<String> scopeFilePaths = new HashSet<>();
            for (Edge edge : useEdges) {
              Node sourceNode = graph.getEdgeSource(edge);
              if (sourceNode.getLanguage().equals(Language.JAVA)) {
                Triple<String, String, String> entities = findWrappedEntities(graph, sourceNode);
                if (!entities.getLeft().isEmpty()) {
                  scopeFilePaths.add(entities.getLeft());
                }
              }
            }

            // cached binding infos of relevant nodes: co-change candidates
            Map<String, Binding> bindingInfos = new HashMap<>();
            // for each diff in the current file
            for (XMLDiff diff : entry.getValue()) {
              if (XMLDiffUtil.isIDLabel(diff.getName())) {
                String elementID = diff.getName().replace("\"", "").replace("+", "");
                Optional<ElementNode> nodeOpt =
                    XMLNodes.stream()
                        .filter(node -> elementID.equals(node.getQualifiedName()))
                        .findAny();

                Map<String, Double> contextNodes = new LinkedHashMap<>();

                // locate changed xml nodes in the graph
                if (nodeOpt.isPresent()) {
                  // if exist, modified/removed
                  ElementNode node = nodeOpt.get();
                  String nodeID = node.getQualifiedName();

                  contextNodes.put(viewID, 1D);
                  contextNodes.put(nodeID, 1D);

                  if (!bindingInfos.containsKey(nodeID)) {
                    bindingInfos.put(nodeID, inferReferences(graph, node, scopeFilePaths));
                  }
                } else {
                  // if not exist/added: predict co-changes according to similar nodes
                  if (diff.getChangeType().equals(ChangeType.ADDED)) {
                    contextNodes = diff.getContextNodes();
                  }
                }

                // all have its parent as a context
                contextNodes.put(viewID, 1D);

                for (Map.Entry<String, Double> cxtEntry : contextNodes.entrySet()) {
                  String siblingID = cxtEntry.getKey();

                  Optional<ElementNode> cxtOpt =
                      XMLNodes.stream()
                          .filter(node -> siblingID.equals(node.getQualifiedName()))
                          .findAny();
                  cxtOpt.ifPresent(
                      elementNode -> {
                        if (!bindingInfos.containsKey(siblingID)) {
                          bindingInfos.put(
                              siblingID, inferReferences(graph, elementNode, scopeFilePaths));
                        }
                      });
                }
                // compute for the current diff
                collaborativeFilter(bindingInfos, contextNodes);
              }
            }
          }
          // measure accuracy by comparing with ground truth
          evaluate(javaDiffs);
          //          cochangeFiles.forEach(System.out::printf);
          //          cochangeTypes.forEach(System.out::printf);
          //          cochangeMembers.forEach(System.out::printf);
        }
      }
    }
  }

  private static boolean hasViewChanges(JSONObject commit) {
    if (commit == null) {
      return false;
    }
    JSONArray diffFiles = (JSONArray) commit.get("diff_files");
    for (JSONObject diffFile : (Iterable<JSONObject>) diffFiles) {
      if (((String) diffFile.get("file_path")).contains("layout")) {
        return true;
      }
    }
    return false;
  }

  /** @param groundTruth */
  private static void evaluate(Map<String, List<JavaDiff>> groundTruth) {

    // Ground Truth
    Set<String> gtAllFiles = new HashSet<>();
    Set<String> gtAllTypes = new HashSet<>();
    Set<String> gtAllMembers = new HashSet<>();
    for (Map.Entry<String, List<JavaDiff>> entry : groundTruth.entrySet()) {
      gtAllFiles.add(entry.getKey());
      for (JavaDiff diff : entry.getValue()) {
        if (!diff.getType().isEmpty()) {
          gtAllTypes.add(diff.getType());
        }

        if (!diff.getMember().isEmpty()) {
          gtAllMembers.add(diff.getMember());
        }
      }
    }

    // separately on three levels
    compare("File level", gtAllFiles, cochangeFiles);
    compare("Type level", gtAllTypes, cochangeTypes);
    compare("Member level", gtAllMembers, cochangeMembers);
  }

  private static void compare(
      String message, Set<String> groundTruth, Map<String, Double> suggestion) {
    List<Pair<String, Double>> output = sortMapByValue(suggestion);
    int groundTruthNum = groundTruth.size();
    int outputNum = output.size();

    Set<String> outputEntries = output.stream().map(Pair::getLeft).collect(Collectors.toSet());

    int correctNum = MetricUtil.intersectSize(groundTruth, outputEntries);

    // order not considered
    // precision and recall
    System.out.print(
        message
            + ": "
            + "Precision="
            + computeMetric(correctNum, outputNum)
            + " Recall="
            + computeMetric(correctNum, groundTruthNum)
            + " ");

    // order considered: MAP
    double sum = 0D;
    double correctForK = 0D;
    double recallForPreviousK = 0D;
    for (int k = 1; k <= outputNum; k++) {
      if (groundTruth.contains(output.get(k - 1).getLeft())) {
        correctForK += 1;
      }
      double precisionForK = correctForK / k;
      double deltaRecallForK = correctForK / groundTruthNum - recallForPreviousK;
      sum += precisionForK * deltaRecallForK;
    }
    System.out.println("Average Precision=" + sum);
  }

  private static double computeMetric(int a, int b) {
    return b == 0 ? 1D : MetricUtil.formatDouble((double) a / b);
  }

  /**
   * Evaluate and rank the co-change probability with neighborhood-based&user-based CF algorithm
   * from recommendation system
   */
  private static void collaborativeFilter(
      Map<String, Binding> bindingInfos, Map<String, Double> contextNodes) {
    Map<String, Map<String, Integer>> fileLookup = new HashMap<>();
    Map<String, Map<String, Integer>> typeLookup = new HashMap<>();
    Map<String, Map<String, Integer>> memberLookup = new HashMap<>();

    for (Map.Entry<String, Binding> entry : bindingInfos.entrySet()) {
      String id = entry.getKey();
      Binding binding = entry.getValue();
      buildLookup(fileLookup, id, binding.getFiles());
      buildLookup(typeLookup, id, binding.getTypes());
      buildLookup(memberLookup, id, binding.getMembers());
    }

    mergeOutputEntry(cochangeFiles, generateCochange(fileLookup, contextNodes));
    mergeOutputEntry(cochangeTypes, generateCochange(typeLookup, contextNodes));
    mergeOutputEntry(cochangeMembers, generateCochange(memberLookup, contextNodes));
  }

  private static void mergeOutputEntry(
      Map<String, Double> output, List<Pair<String, Double>> cochanges) {
    for (var pair : cochanges) {
      if (!output.containsKey(pair.getLeft())) {
        output.put(pair.getLeft(), 0D);
      }
      Double oldConfidence = output.get(pair.getLeft());
      output.put(pair.getLeft(), oldConfidence + pair.getRight());
    }
  }

  private static List<Pair<String, Double>> generateCochange(
      Map<String, Map<String, Integer>> lookup, Map<String, Double> contextNodes) {
    List<Pair<String, Double>> results = new ArrayList<>();

    for (var entityEntry : lookup.entrySet()) {
      Map<String, Integer> reverseRefs = entityEntry.getValue();
      double sum1 = 0D;
      double sum2 = 0D;
      for (var refEntry : reverseRefs.entrySet()) {
        String id = refEntry.getKey();
        if (contextNodes.containsKey(id)) {
          sum1 += (contextNodes.get(id) * refEntry.getValue());
          sum2 += contextNodes.get(id);
        }
      }
      double confidence = sum1 / sum2;
      if (confidence > 0) {
        results.add(Pair.of(entityEntry.getKey(), confidence));
      }
    }
    return results;
  }

  /**
   * Build a reverse lookup table: from entry to id
   *
   * @param lookup
   * @param id
   * @param refs
   */
  private static void buildLookup(
      Map<String, Map<String, Integer>> lookup, String id, Map<String, Integer> refs) {
    for (Map.Entry<String, Integer> entry : refs.entrySet()) {
      if (!lookup.containsKey(entry.getKey())) {
        lookup.put(entry.getKey(), new HashMap<>());
      }
      lookup.get(entry.getKey()).put(id, entry.getValue());
    }
  }

  private static List<Pair<String, Double>> sortMapByValue(Map<String, Double> map) {
    List<Map.Entry<String, Double>> entryList = new ArrayList<>(map.entrySet());
    // descending order
    entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    List<Pair<String, Double>> results = new ArrayList<>();
    entryList.forEach(e -> results.add(Pair.of(e.getKey(), e.getValue())));
    return results;
  }

  private static Binding inferReferences(
      Graph<Node, Edge> graph, ElementNode node, Set<String> scopeFilePaths) {
    Binding binding = new Binding(node.getQualifiedName());

    Set<Edge> useEdges = getUseEdges(graph, node);

    // TODO: express the change for added elements
    for (Edge useEdge : useEdges) {
      Node refNode = graph.getEdgeSource(useEdge);
      if (refNode.getLanguage().equals(Language.JAVA)) {
        Set<Node> relevantNodes = new HashSet<>();
        relevantNodes.add(refNode);

        // find indirect uses with dynamic hop stop condition
        relevantNodes.addAll(getIndirectNodes(graph, refNode));
        // filter incorrect references without the correct view
        // find wrapped member, type, and file nodes, return names
        for (Node rNode : relevantNodes) {
          Triple<String, String, String> entities = findWrappedEntities(graph, rNode);
          if (scopeFilePaths.contains(entities.getLeft())) {
            binding.addRefEntities(entities);
          }
        }
      }
    }
    return binding;
  }

  private static Set<Node> getIndirectNodes(Graph<Node, Edge> graph, Node refNode) {
    Set<Node> results = new HashSet<>();
    Optional<Object> accessOpt = refNode.getAttribute("access");
    if (accessOpt.isPresent()) {
      // TODO: determine dynamically by hop
      //    switch (access) {
      //      case "private":
      //        // find all indirect refs under the same type
      //        break;
      //      default:
      //    }
      Set<Edge> useEdges = getUseEdges(graph, refNode);
      for (Edge e : useEdges) {
        Node n = graph.getEdgeSource(e);
        results.add(n);
      }
    }
    return results;
  }

  private static Set<Edge> getUseEdges(Graph<Node, Edge> graph, Node node) {
    return graph.incomingEdgesOf(node).stream()
        .filter(edge -> !edge.getType().equals(EdgeType.CHILD))
        .collect(Collectors.toSet());
  }

  /**
   * Find (file relative path, type name, member name) for Java nodes
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
          filePath = FileUtil.getRelativePath(repoPath, ((ElementNode) parent).getQualifiedName());
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
    List<String> allJavaFilePaths = FileUtil.listFilePaths(repoPath, ".java");
    //    logger.info("Total Java files: "+allJavaFilePaths.size());

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

    logger.info("Java files: " + filePaths.size());

    // collect all xml layout files
    Set<String> xmlFilePaths =
        FileUtil.listFilePaths(repoPath, ".xml").stream()
            .filter(path -> path.contains("layout"))
            .collect(Collectors.toSet());

    logger.info("XML files: " + xmlFilePaths.size());

    filePaths.addAll(xmlFilePaths);

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
