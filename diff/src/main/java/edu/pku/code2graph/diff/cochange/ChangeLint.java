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

  private static String repoName = "EhViewer";
  //  private static String repoName = "test_repo";
  private static String repoPath = "/Users/symbolk/coding/data/repos/" + repoName;
  private static String tempDir = "/Users/symbolk/coding/data/temp/c2g";

  private static PriorityQueue<Pair<String, Double>> cochangeFiles =
      new PriorityQueue<>(new PairComparator());
  private static PriorityQueue<Pair<String, Double>> cochangeTypes =
      new PriorityQueue<>(new PairComparator());
  private static PriorityQueue<Pair<String, Double>> cochangeMembers =
      new PriorityQueue<>(new PairComparator());

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

    // TODO: only consider the changes that involves id changes (for input commits)
    String testCommitID = "db893b8";

    // 1. Offline process: given the commit id of the earliest future multi-lang commit
    // checkout to that version
    //    SysUtil.runSystemCommand(repoPath, Charset.defaultCharset(), "git", "checkout", "-b",
    // "changelint", "e457da8");

    // checkout to the previous version
    SysUtil.runSystemCommand(
        repoPath, Charset.defaultCharset(), "git", "checkout", testCommitID + "~");

    //   build the graph for the current version
    Graph<Node, Edge> graph = buildGraph();
    logger.info(
        "Graph building done, with {} nodes and {} edges",
        graph.vertexSet().size(),
        graph.edgeSet().size());

    // 2. Online process: for each of the future commits, extract the changes as GT
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(testCommitID);
    //    DataCollector dataCollector = new DataCollector(tempDir);
    //    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);

    // Input: XMLDiff (file relative path, changed xml element id)
    Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
    // Ground Truth: JavaDiff (file relative path, type name, member name)
    Map<String, List<JavaDiff>> javaDiffs = new HashMap<>();

    for (DiffFile diffFile : diffFiles) {
      if (diffFile.getFileType().equals(FileType.XML)
          && diffFile.getARelativePath().contains("layout")) {
        xmlDiffs.put(
            diffFile.getARelativePath(), XMLDiffUtil.computeXMLChangesWithGumtree(diffFile));
        //        xmlDiffs.put(
        //            diffFile.getARelativePath(),
        //                XMLDiffUtil.computeXMLChanges(
        //                tempFilePaths, diffFile.getARelativePath(), diffFile.getBRelativePath()));
      } else if (diffFile.getFileType().equals(FileType.JAVA)) {
        List<JavaDiff> changes = JavaDiffUtil.computeJavaChanges(diffFile);
        if (!changes.isEmpty()) {
          // ignore files with only format/comment changes
          javaDiffs.put(diffFile.getARelativePath(), changes);
        }
      }
    }

    // Output: predicted co-change file/type/member in the graph
    //    Map<String, List<JavaDiff>> suggestedCoChanges = new HashMap<>();
    // check if exists to determine change action (if the tool reports wrong change action)
    Set<ElementNode> XMLNodes =
        graph.vertexSet().stream()
            .filter(v -> v.getLanguage().equals(Language.XML))
            .filter(v -> v instanceof ElementNode)
            .map(ElementNode.class::cast)
            .collect(Collectors.toSet());
    for (Map.Entry<String, List<XMLDiff>> entry : xmlDiffs.entrySet()) {
      for (XMLDiff diff : entry.getValue()) {
        if (XMLDiffUtil.isIDLabel(diff.getName())) {
          String elementID = diff.getName().replace("\"", "").replace("+", "");
          Optional<ElementNode> nodeOpt =
              XMLNodes.stream().filter(node -> elementID.equals(node.getQualifiedName())).findAny();
          if (nodeOpt.isPresent()) {
            // locate changed xml nodes in the graph
            // if exist, modified/removed
            Map<String, Binding> bindingInfos = new HashMap<>();
            ElementNode node = nodeOpt.get();
            bindingInfos.put(node.getQualifiedName(), inferReferences(graph, node));
            Map<String, Double> contextNodes = new LinkedHashMap<>();

            collaborativeFilter(bindingInfos, contextNodes);
          } else {
            // if not exist/added: predict co-changes according to similar nodes
            if (diff.getChangeType().equals(ChangeType.ADDED)) {
              Map<String, Double> contextNodes = diff.getContextNodes();
              // find all co-changes of siblings
              Map<String, Binding> bindingInfos = new HashMap<>();
              for (Map.Entry<String, Double> cxtEntry : contextNodes.entrySet()) {
                String siblingID = cxtEntry.getKey().replace("\"", "").replace("+", "");

                Optional<ElementNode> cxtOpt =
                    XMLNodes.stream()
                        .filter(node -> siblingID.equals(node.getQualifiedName()))
                        .findAny();
                cxtOpt.ifPresent(
                    elementNode ->
                        bindingInfos.put(siblingID, inferReferences(graph, elementNode)));
              }
              // compare and count to estimate confidence
              // rank and filter with intersection (algorithm to estimate the relevance/possibility
              // of co-changing)
              // report the suggested co-changes with an ordered list top-k
              collaborativeFilter(bindingInfos, contextNodes);
            }
          }
        }
      }
    }
    // measure accuracy by comparing with ground truth (compute the three sets)
    evaluate(javaDiffs);
  }

  /**
   * TODO evaluate top-k precision
   *
   * @param groundTruth
   */
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
    int otTotalFileNum = cochangeFiles.size();
    int otTotalTypeNum = cochangeTypes.size();
    int otTotalMemberNum = cochangeMembers.size();

    Set<String> otAllFiles = cochangeFiles.stream().map(Pair::getLeft).collect(Collectors.toSet());
    Set<String> otAllTypes = cochangeTypes.stream().map(Pair::getLeft).collect(Collectors.toSet());
    Set<String> otAllMembers =
        cochangeMembers.stream().map(Pair::getLeft).collect(Collectors.toSet());

    int correctFileNum = MetricUtil.intersectSize(gtAllFiles, otAllFiles);
    int correctTypeNum = MetricUtil.intersectSize(gtAllTypes, otAllTypes);
    int correctMemberNum = MetricUtil.intersectSize(gtAllMembers, otAllMembers);

    // precision
    System.out.println(computeProportion(correctFileNum, otTotalFileNum));
    System.out.println(computeProportion(correctTypeNum, otTotalTypeNum));
    System.out.println(computeProportion(correctMemberNum, otTotalMemberNum));

    // recall
    System.out.println(computeProportion(correctFileNum, gtAllFiles.size()));
    System.out.println(computeProportion(correctTypeNum, gtAllTypes.size()));
    System.out.println(computeProportion(correctMemberNum, gtAllMembers.size()));
  }

  private static double computeProportion(int a, int b) {
    return b == 0 ? 0D : MetricUtil.formatDouble((double) a / b);
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

    // for each entity, find other bindings that has this entry key
    // compute weighted average
    // save and sort
    cochangeFiles = generateCochange(fileLookup, contextNodes);
    cochangeTypes = generateCochange(typeLookup, contextNodes);
    cochangeMembers = generateCochange(memberLookup, contextNodes);
  }

  private static PriorityQueue<Pair<String, Double>> generateCochange(
      Map<String, Map<String, Integer>> lookup, Map<String, Double> contextNodes) {
    PriorityQueue<Pair<String, Double>> pq = new PriorityQueue<>(new PairComparator());

    for (var entityEntry : lookup.entrySet()) {
      Map<String, Integer> reverseRefs = entityEntry.getValue();
      double sum1 = 0D;
      double sum2 = 0D;
      for (var refEntry : reverseRefs.entrySet()) {
        String id = refEntry.getKey();
        sum1 += (contextNodes.get(id) * refEntry.getValue());
        sum2 += contextNodes.get(id);
      }
      pq.add(Pair.of(entityEntry.getKey(), sum1 / sum2));
    }
    return pq;
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

  private static List<Pair<String, Double>> convertMapToList(Map<String, Double> map) {
    List<Map.Entry<String, Double>> entryList = new ArrayList<>(map.entrySet());
    entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    List<Pair<String, Double>> results = new ArrayList<>();
    entryList.forEach(e -> results.add(Pair.of(e.getKey(), e.getValue())));
    return results;
  }

  private static Binding inferReferences(Graph<Node, Edge> graph, ElementNode node) {
    Binding binding = new Binding(node.getQualifiedName());

    Set<Edge> useEdges =
        graph.incomingEdgesOf(node).stream()
            .filter(edge -> !edge.getType().equals(EdgeType.CHILD))
            .collect(Collectors.toSet());

    // TODO: find edge source (uses) nodes in Java code k hops
    // TODO: filter or correct the references with R.layout.X
    // TODO: express the change for added elements
    for (Edge useEdge : useEdges) {
      Node sourceNode = graph.getEdgeSource(useEdge);
      if (sourceNode.getLanguage().equals(Language.JAVA)) {
        // find wrapped member, type, and file nodes, return names
        Triple<String, String, String> entities = findWrappedEntities(graph, sourceNode);
        binding.addRefEntities(entities);
      }
    }
    return binding;
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
    List<String> allJavaFilePaths = FileUtil.getSpecificFilePaths(repoPath, ".java");
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
        FileUtil.getSpecificFilePaths(repoPath, ".xml").stream()
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
