package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.ChangeType;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.FileType;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
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
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.PropertyConfigurator;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
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
  private static String repoName = "";
  private static String repoPath = "";
  private static final String tempDir = rootFolder + "/temp";

  private static String commitsListPath = rootFolder + "/cross-lang-commits/eh";

  private static List<Suggestion> suggestedFiles = new ArrayList<>();
  private static List<Suggestion> suggestedTypes = new ArrayList<>();
  private static List<Suggestion> suggestedMembers = new ArrayList<>();

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
    List<String> filePaths = FileUtil.listFilePaths(commitsListPath, ".json");

    // one file, one repo
    for (String filePath : filePaths) {
      repoName = FileUtil.getFileNameFromPath(filePath).replace(".json", "");
      repoPath = rootFolder + "/repos/" + repoName;
      RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);

      JSONParser parser = new JSONParser();
      JSONArray commitList = (JSONArray) parser.parse(new FileReader(filePath));

      // one entry, one commit
      for (JSONObject commit : (Iterable<JSONObject>) commitList) {
        //          String testCommitID = (String) commit.get("commit_id");
        String testCommitID = "db893b8";
        // 1. Offline process: given the commit id of the earliest future multi-lang commit
        logger.info("Processing repo: {} at {}", repoName, repoPath);

        logger.info("Computing diffs for commit: {}", testCommitID);
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

        logger.info("XML diff files: {} for commit: {}", xmlDiffs.entrySet().size(), testCommitID);
        logger.info(
            "Java diff files: {} for commit: {}", javaDiffs.entrySet().size(), testCommitID);

        //          logger.info("Computing historical cochanges...");
        //          computeHistoricalCochanges(xmlDiffs);

        suggestedFiles = new ArrayList<>();
        suggestedTypes = new ArrayList<>();
        suggestedMembers = new ArrayList<>();

        // checkout to the previous version
        logger.info("Checking out to previous commit of {}", testCommitID);
        SysUtil.runSystemCommand(
            repoPath,
            Charset.defaultCharset(),
            "git",
            "checkout", /* "-b","changelint", */
            testCommitID + "~");

        logger.info("Building graph...");
        //   build the graph for the current version
        Graph<Node, Edge> graph = buildGraph();

        logger.info(
            "Graph building done, nodes: {}; edges: {}",
            graph.vertexSet().size(),
            graph.edgeSet().size());

        // 2. Online process: for each of the future commits, extract the changes as GT

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

                contextNodes.put(nodeID, 1D);

                if (!bindingInfos.containsKey(nodeID)) {
                  bindingInfos.put(nodeID, inferReferences(graph, node, scopeFilePaths));
                }
                generateSuggestion(bindingInfos);
              } else {
                // if not exist/added: predict co-changes according to similar nodes
                if (diff.getChangeType().equals(ChangeType.ADDED)) {
                  contextNodes = diff.getContextNodes();
                }

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
                generateSuggestion(bindingInfos, contextNodes);
              }
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

  private static void evaluate(Map<String, List<JavaDiff>> groundTruth) {

    // Ground Truth
    Set<Suggestion> gtAllFiles = new HashSet<>();
    Set<Suggestion> gtAllTypes = new HashSet<>();
    Set<Suggestion> gtAllMembers = new HashSet<>();
    for (Map.Entry<String, List<JavaDiff>> entry : groundTruth.entrySet()) {
      gtAllFiles.add(new Suggestion(ChangeType.UPDATED, EntityType.FILE, entry.getKey(), 1.0));
      for (JavaDiff diff : entry.getValue()) {
        if (diff.getMember().isEmpty()) {
          gtAllTypes.add(
              new Suggestion(diff.getChangeType(), EntityType.TYPE, diff.getType(), 1.0));
        } else {
          gtAllMembers.add(
              new Suggestion(diff.getChangeType(), EntityType.MEMBER, diff.getMember(), 1.0));
          if (!diff.getType().isEmpty()) {
            gtAllTypes.add(
                new Suggestion(ChangeType.UPDATED, EntityType.TYPE, diff.getType(), 1.0));
          }
        }
      }
    }

    // separately on three levels
    compare("File level", gtAllFiles, suggestedFiles);
    compare("Type level", gtAllTypes, suggestedTypes);
    compare("Member level", gtAllMembers, suggestedMembers);
  }

  private static void compare(
      String message, Set<Suggestion> groundTruth, List<Suggestion> suggestions) {
    suggestions.sort(new SuggestionComparator());
    int groundTruthNum = groundTruth.size();
    int outputNum = suggestions.size();

    //    Set<String> outputEntries =
    //        suggestions.stream().map(Suggestion::getIdentifier).collect(Collectors.toSet());
    //    MetricUtil.intersectSize(groundTruth, outputEntries);

    int correctNum = 0;
    for (Suggestion sg : suggestions) {
      if (hitGroundTruth(groundTruth, sg)) {
        correctNum += 1;
      }
    }

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
      if (hitGroundTruth(groundTruth, suggestions.get(k - 1))) {
        correctForK += 1;
      }
      double precisionForK = correctForK / k;
      double recallForK = correctForK / groundTruthNum;
      sum += precisionForK * (recallForK - recallForPreviousK);
      recallForPreviousK = recallForK;
    }
    System.out.println("Average Precision=" + MetricUtil.formatDouble(sum));
  }

  /**
   * Check if the suggestion hit any in ground truth
   *
   * @param groundTruth
   * @param suggestion
   * @return
   */
  private static boolean hitGroundTruth(Set<Suggestion> groundTruth, Suggestion suggestion) {
    if (suggestion.getChangeType().equals(ChangeType.ADDED)) {
      // for added, identifier is not important and cannot be precisely predicted
      for (Suggestion gt : groundTruth) {
        if (gt.getChangeType().equals(suggestion.getChangeType())
            && gt.getEntityType().equals(suggestion.getEntityType())) {
          return true;
        }
      }
    } else {
      for (Suggestion gt : groundTruth) {
        if (gt.getIdentifier().equals(suggestion.getIdentifier())) {
          return true;
        }
      }
    }
    return false;
  }

  private static double computeMetric(int a, int b) {
    return b == 0 ? 1D : MetricUtil.formatDouble((double) a / b);
  }

  private static void generateSuggestion(Map<String, Binding> bindingInfos) {
    for (Map.Entry<String, Binding> entry : bindingInfos.entrySet()) {
      String id = entry.getKey();
      Binding binding = entry.getValue();
      for (var temp : binding.getFiles().entrySet()) {
        suggestedFiles.add(
            new Suggestion(ChangeType.UPDATED, EntityType.FILE, temp.getKey(), temp.getValue()));
      }
      for (var temp : binding.getTypes().entrySet()) {
        suggestedTypes.add(
            new Suggestion(ChangeType.UPDATED, EntityType.TYPE, temp.getKey(), temp.getValue()));
      }
      for (var temp : binding.getMembers().entrySet()) {
        suggestedMembers.add(
            new Suggestion(ChangeType.UPDATED, EntityType.MEMBER, temp.getKey(), temp.getValue()));
      }
    }
  }

  /**
   * Evaluate and rank the co-change probability with neighborhood-based&user-based CF algorithm
   * from recommendation system
   */
  private static void generateSuggestion(
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

    mergeOutputEntry(
        suggestedFiles, collaborativeFilter(EntityType.FILE, fileLookup, contextNodes));
    mergeOutputEntry(
        suggestedTypes, collaborativeFilter(EntityType.TYPE, typeLookup, contextNodes));
    mergeOutputEntry(
        suggestedMembers, collaborativeFilter(EntityType.MEMBER, memberLookup, contextNodes));
  }

  private static void mergeOutputEntry(List<Suggestion> output, Set<Suggestion> suggestions) {
    for (var sug : suggestions) {
      if (sug.getIdentifier().isEmpty() || sug.getIdentifier().isBlank()) {
        output.add(sug);
      } else {
        // if exist, change confidence
        boolean exist = false;
        for (Suggestion ot : output) {
          if (ot.getEntityType().equals(ot.getEntityType())
              && ot.getIdentifier().equals(sug.getIdentifier())) {
            ot.setConfidence(ot.getConfidence() + sug.getConfidence());
            exist = true;
          }
          break;
        }
        // else, add a new
        if (!exist) {
          output.add(sug);
        }
      }
    }
  }

  private static Set<Suggestion> collaborativeFilter(
      EntityType entityType,
      Map<String, Map<String, Integer>> lookup,
      Map<String, Double> contextNodes) {
    Set<Suggestion> results = new HashSet<>();

    for (var entityEntry : lookup.entrySet()) {
      Map<String, Integer> reverseRefs = entityEntry.getValue();
      double sum1 = 0D;
      double sum2 = 0D;
      for (var id : contextNodes.keySet()) {
        sum1 += (contextNodes.get(id) * reverseRefs.getOrDefault(id, -1));
        sum2 += contextNodes.get(id);
      }
      //      for (var refEntry : reverseRefs.entrySet()) {
      //        String id = refEntry.getKey();
      //        if (contextNodes.containsKey(id)) {
      //          sum1 += (contextNodes.get(id) * refEntry.getValue());
      //          sum2 += contextNodes.get(id);
      //      }
      double confidence = MetricUtil.formatDouble(sum1 / sum2);
      if (confidence > 0) {
        results.add(
            new Suggestion(ChangeType.UPDATED, entityType, entityEntry.getKey(), confidence));
      } else {
        results.add(new Suggestion(ChangeType.ADDED, entityType, "[]", confidence));
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

  static class SuggestionComparator implements Comparator<Suggestion> {
    // descending order
    @Override
    public int compare(Suggestion s1, Suggestion s2) {
      if (s1.getConfidence() < s2.getConfidence()) {
        return 1;
      } else if (s1.getConfidence() > s2.getConfidence()) {
        return -1;
      }
      return 0;
    }
  }

  //  private static List<Pair<String, Double>> sortMapByValue(Map<String, Double> map) {
  //    List<Map.Entry<String, Double>> entryList = new ArrayList<>(map.entrySet());
  //    // descending order
  //    entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
  //    List<Pair<String, Double>> results = new ArrayList<>();
  //    entryList.forEach(e -> results.add(Pair.of(e.getKey(), e.getValue())));
  //    return results;
  //  }

  private static Binding inferReferences(
      Graph<Node, Edge> graph, ElementNode node, Set<String> scopeFilePaths) {
    Binding binding = new Binding(node.getQualifiedName());

    Set<Edge> useEdges = getUseEdges(graph, node);

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

  /**
   * Get all connected nodes (k hops or dynamic hops)
   *
   * @param graph
   * @param refNode
   * @return
   */
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
      Set<Node> neighbors =
          Graphs.neighborListOf(graph, refNode).stream()
              .filter(n -> n.getLanguage().equals(Language.JAVA))
              .collect(Collectors.toSet());
      results.addAll(neighbors);
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

  /** Compute evolutionary coupling entities from the past commits */
  private static void computeHistoricalCochanges(Map<String, List<XMLDiff>> xmlDiffs) {
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);

    Map<String, Map<String, Double>> cochangeFiles = new HashMap<>();
    Map<String, Map<String, Double>> cochangeTypes = new HashMap<>();
    Map<String, Map<String, Double>> cochangeMembers = new HashMap<>();

    // get all commits that ever changed each xml file
    for (String xmlFilePath : xmlDiffs.keySet()) {
      GitService gitService = new GitServiceCGit();
      // note that here "HEAD" is the tested commit, since we have checkout to it before
      List<String> commitIDs = gitService.getCommitsChangedFile(repoPath, xmlFilePath, "HEAD", 10);
      int numAllCommits = commitIDs.size();
      // count the number of co-change commits
      Map<String, Double> filesCounter = new HashMap<>();
      Map<String, Double> typesCounter = new HashMap<>();
      Map<String, Double> membersCounter = new HashMap<>();

      for (String commitID : commitIDs) {
        // extract co-changing entities at 3 levels
        Map<String, List<JavaDiff>> javaDiffs = new HashMap<>();

        List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(commitID);
        for (DiffFile diffFile : diffFiles) {
          if (diffFile.getFileType().equals(FileType.JAVA)) {
            List<JavaDiff> changes = JavaDiffUtil.computeJavaChanges(diffFile);
            if (!changes.isEmpty()) {
              // ignore files with only format/comment changes
              javaDiffs.put(
                  diffFile.getARelativePath().isBlank()
                      ? diffFile.getBRelativePath()
                      : diffFile.getARelativePath(),
                  changes);
            }
          }
        }

        // count number of cochange commits
        for (var entry : javaDiffs.entrySet()) {
          List<JavaDiff> diffs = entry.getValue();
          String diffFilePath = entry.getKey();
          if (!diffFilePath.isEmpty()) {
            if (!filesCounter.containsKey(diffFilePath)) {
              filesCounter.put(diffFilePath, 0D);
            }
            filesCounter.put(diffFilePath, filesCounter.get(diffFilePath) + 1);
          }
          for (JavaDiff diff : diffs) {
            String diffType = diff.getType();
            if (!diffType.isEmpty()) {
              if (!typesCounter.containsKey(diffType)) {
                typesCounter.put(diffType, 0D);
              }
              typesCounter.put(diffType, typesCounter.get(diffType) + 1);
            }

            String diffMember = diff.getMember();
            if (!diffMember.isEmpty()) {
              if (!membersCounter.containsKey(diffMember)) {
                membersCounter.put(diffMember, 0D);
              }
              membersCounter.put(diffMember, membersCounter.get(diffMember) + 1);
            }
          }
        }
      }
      // store and consume
      // compute confidence for each entity
      filesCounter.replaceAll((k, v) -> MetricUtil.formatDouble(v / numAllCommits));
      typesCounter.replaceAll((k, v) -> MetricUtil.formatDouble(v / numAllCommits));
      membersCounter.replaceAll((k, v) -> MetricUtil.formatDouble(v / numAllCommits));
      cochangeFiles.put(xmlFilePath, filesCounter);
      cochangeTypes.put(xmlFilePath, typesCounter);
      cochangeMembers.put(xmlFilePath, membersCounter);
    }
  }

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
