package edu.pku.code2graph.diff.cochange;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.ChangeType;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.FileType;
import edu.pku.code2graph.diff.util.Counter;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class ChangeLint {
  static Logger logger = LoggerFactory.getLogger(ChangeLint.class);

  private static final String rootFolder = "/Users/symbolk/coding/changelint";
  private static String repoName = "";
  private static String repoPath = "";
  private static final String tempDir = rootFolder + "/temp";
  private static final String outputDir = rootFolder + "/output";

  private static String commitsListDir = rootFolder + "/input";

  private static SortedSet<Suggestion> suggestedFiles = new TreeSet<>(new SuggestionComparator());
  private static SortedSet<Suggestion> suggestedTypes = new TreeSet<>(new SuggestionComparator());
  private static SortedSet<Suggestion> suggestedMembers = new TreeSet<>(new SuggestionComparator());

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

  private static void collectChangesForRepo() throws IOException {
    logger.info("Collecting data for repo: {} at {}", repoName, repoPath);
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    // input
    String commitListFilePath = commitsListDir + File.separator + repoName + ".json";
    FileUtil.clearDir(tempDir + File.separator + repoName);

    JSONParser parser = new JSONParser();
    JSONArray commitList = new JSONArray();
    try (FileReader reader = new FileReader(commitListFilePath)) {
      commitList = (JSONArray) parser.parse(reader);
    } catch (IOException | ParseException e) {
      e.printStackTrace();
    }

    // one entry, one commit
    for (JSONObject commit : (Iterable<JSONObject>) commitList) {
      String commitID = (String) commit.get("commit_id");
      // output
      String outputPath = tempDir + File.separator + commitID + ".json";

      logger.info("Computing diffs for commit: {}", commitID);
      List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(commitID);
      //    DataCollector dataCollector = new DataCollector(tempDir);
      //    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);

      Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
      Map<String, List<JavaDiff>> javaDiffs = new HashMap<>();

      Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

      for (DiffFile diffFile : diffFiles) {
        if (diffFile.getFileType().equals(FileType.XML)
            && diffFile.getARelativePath().contains("layout")) {
          List<XMLDiff> changes = XMLDiffUtil.computeXMLChangesWithGumtree(diffFile);
          if (!changes.isEmpty()) {
            xmlDiffs.put(diffFile.getARelativePath(), changes);
          }
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

      // jump commits with pure move/comments/format changes
      if (xmlDiffs.isEmpty() || javaDiffs.isEmpty()) {
        continue;
      }

      logger.info("XML diff files: {} for commit: {}", xmlDiffs.entrySet().size(), commitID);
      logger.info("Java diff files: {} for commit: {}", javaDiffs.entrySet().size(), commitID);

      JSONObject outputJson = new JSONObject(new LinkedHashMap());
      JSONObject xmlDiffJson = new JSONObject(new LinkedHashMap());
      JSONObject javaDiffJson = new JSONObject(new LinkedHashMap());

      outputJson.put("commit_id", commitID);

      for (var entry : xmlDiffs.entrySet()) {
        JSONArray array = new JSONArray();
        for (var diff : entry.getValue()) {
          array.add(gson.toJsonTree(diff));
        }
        xmlDiffJson.put(entry.getKey(), array);
      }

      for (var entry : javaDiffs.entrySet()) {
        JSONArray array = new JSONArray();
        for (var diff : entry.getValue()) {
          array.add(gson.toJsonTree(diff));
        }
        javaDiffJson.put(entry.getKey(), array);
      }

      outputJson.put("xml_diff", xmlDiffJson);
      outputJson.put("java_diff", javaDiffJson);

      try (FileWriter file = new FileWriter(outputPath.toString(), false)) {
        JSONObject.writeJSONString(outputJson, file);
      }
    }
  }

  public static void main(String[] args) throws IOException {

    //    BasicConfigurator.configure();
    PropertyConfigurator.configure(
        System.getProperty("user.dir") + File.separator + "log4j.properties");
    repoName = "youlookwhat-CloudReader";
    repoPath = rootFolder + "/repos/" + repoName;

    // run only once to collect data
    //    collectChangesForRepo();

    logger.info("Processing repo: {} at {}", repoName, repoPath);
    List<String> dataFilePaths =
        FileUtil.listFilePaths(tempDir + File.separator + repoName, ".json");
    String outputPath = outputDir + File.separator + repoName + ".json";

    File outputFile = new File(outputPath);
    if (outputFile.exists()) {
      outputFile.delete();
    }
    FileUtil.writeStringToFile("[", outputPath);

    Gson gson = new Gson();
    // Input: XMLDiff (file relative path: <file relative path, changed xml element type, id>)
    Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
    // Ground Truth: JavaDiff (file relative path: <file relative path, type name, member
    // name>)
    Map<String, List<JavaDiff>> javaDiffs = new HashMap<>();

    for (String dataFilePath : dataFilePaths) {
      String testCommitID = FileUtil.getFileNameFromPath(dataFilePath).replace(".json", "");
      JSONParser parser = new JSONParser();
      try (FileReader reader = new FileReader(dataFilePath)) {
        JSONObject obj = (JSONObject) parser.parse(reader);
        JSONObject xmlObj = (JSONObject) obj.get("xml_diff");
        for (Iterator iterator = xmlObj.keySet().iterator(); iterator.hasNext(); ) {
          String key = (String) iterator.next();
          List<XMLDiff> diffs = new ArrayList<>();
          ((JSONArray) xmlObj.get(key))
              .forEach(
                  item -> {
                    diffs.add(gson.fromJson(item.toString(), XMLDiff.class));
                  });
          xmlDiffs.put(key, diffs);
        }

        JSONObject javaObj = (JSONObject) obj.get("java_diff");
        for (Iterator iterator = javaObj.keySet().iterator(); iterator.hasNext(); ) {
          String key = (String) iterator.next();
          List<JavaDiff> diffs = new ArrayList<>();
          ((JSONArray) javaObj.get(key))
              .forEach(
                  item -> {
                    diffs.add(gson.fromJson(item.toString(), JavaDiff.class));
                  });
          javaDiffs.put(key, diffs);
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }

      logger.info("XML diff files: {} for commit: {}", xmlDiffs.entrySet().size(), testCommitID);
      logger.info("Java diff files: {} for commit: {}", javaDiffs.entrySet().size(), testCommitID);

      //        logger.info("Computing historical cochanges...");
      //        computeHistoricalCochanges(xmlDiffs);

      suggestedFiles.clear();
      suggestedTypes.clear();
      suggestedMembers.clear();

      // checkout to the previous version
      logger.info(
          SysUtil.runSystemCommand(
              repoPath,
              Charset.defaultCharset(),
              "git",
              "checkout", /* "-b","changelint", */
              "-f",
              testCommitID + "^"));

      String parentCommitID =
          SysUtil.runSystemCommand(
              repoPath,
              Charset.defaultCharset(),
              "git",
              "log",
              "--pretty=%P",
              "-n",
              "1",
              testCommitID);

      logger.info(
          "Now at HEAD commit: {}Expected at commit: {}",
          SysUtil.runSystemCommand(repoPath, Charset.defaultCharset(), "git", "rev-parse", "HEAD"),
          parentCommitID);

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
        Optional<ElementNode> viewNodeOpt =
            XMLNodes.stream().filter(node -> viewID.equals(node.getQualifiedName())).findFirst();
        // find the file name that directly/indirectly refs the view node
        Set<String> scopeFilePaths = new HashSet<>();
        if (viewNodeOpt.isPresent()) {
          Set<Edge> useEdges = getUseEdges(graph, viewNodeOpt.get());
          for (Edge edge : useEdges) {
            Node sourceNode = graph.getEdgeSource(edge);
            if (sourceNode.getLanguage().equals(Language.JAVA)) {
              Triple<String, String, String> entities = findWrappedEntities(graph, sourceNode);
              if (!entities.getLeft().isEmpty()) {
                scopeFilePaths.add(entities.getLeft());
              }
            }
          }
        }

        // cached binding infos of relevant nodes: co-change candidates
        Map<String, Binding> bindingInfos = new HashMap<>();
        // for each diff in the current file
        for (XMLDiff diff : entry.getValue()) {
          if (XMLDiffUtil.isIDLabel(diff.getName())) {
            String elementID = diff.getName().replace("\"", "").replace("+", "");
            Optional<ElementNode> diffNodeOpt =
                XMLNodes.stream()
                    .filter(node -> elementID.equals(node.getQualifiedName()))
                    .findAny();

            Map<String, Double> contextNodes = new LinkedHashMap<>();

            // locate changed xml nodes in the graph
            if (diffNodeOpt.isPresent()) {
              // if exist, modified/removed
              ElementNode diffNode = diffNodeOpt.get();
              String nodeID = diffNode.getQualifiedName();

              contextNodes = diff.getContextNodes();
              contextNodes.put(nodeID, 1D);

              if (!bindingInfos.containsKey(nodeID)) {
                bindingInfos.put(nodeID, inferReferences(graph, diffNode, scopeFilePaths));
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

              generateSuggestion(bindingInfos);
            } else {
              // if not exist/added: predict co-changes according to similar nodes
              contextNodes = diff.getContextNodes();

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
      evaluate(testCommitID, outputPath, javaDiffs);
    }
  }

  private static void evaluate(
      String testCommitID, String outputPath, Map<String, List<JavaDiff>> groundTruth)
      throws IOException {

    // Ground Truth
    Set<Suggestion> gtAllFiles = new HashSet<>();
    Set<Suggestion> gtAllTypes = new HashSet<>();
    Set<Suggestion> gtAllMembers = new HashSet<>();
    for (Map.Entry<String, List<JavaDiff>> entry : groundTruth.entrySet()) {
      gtAllFiles.add(new Suggestion(ChangeType.UPDATED, EntityType.FILE, entry.getKey(), 1.0));
      for (JavaDiff diff : entry.getValue()) {
        if (diff.getMember().isEmpty()) {
          gtAllTypes.add(
              new Suggestion(
                  diff.getChangeType(),
                  EntityType.TYPE,
                  diff.getChangeType().equals(ChangeType.ADDED) ? "" : diff.getType(),
                  1.0));
        } else {
          gtAllMembers.add(
              new Suggestion(
                  diff.getChangeType(),
                  EntityType.MEMBER,
                  diff.getChangeType().equals(ChangeType.ADDED) ? "" : diff.getMember(),
                  1.0));
          if (!diff.getType().isEmpty()) {
            gtAllTypes.add(
                new Suggestion(
                    ChangeType.UPDATED,
                    EntityType.TYPE,
                    diff.getChangeType().equals(ChangeType.ADDED) ? "" : diff.getType(),
                    1.0));
          }
        }
      }
    }

    JSONObject outputJson = new JSONObject(new LinkedHashMap());

    outputJson.put("commit_id", testCommitID);

    // separately on three levels
    System.out.println("For commit: " + testCommitID);
    outputJson.put("file", compare("File level", gtAllFiles, suggestedFiles));
    outputJson.put("type", compare("Type level", gtAllTypes, suggestedTypes));
    outputJson.put("member", compare("Member level", gtAllMembers, suggestedMembers));

    try (FileWriter file = new FileWriter(outputPath, true)) {
      JSONObject.writeJSONString(outputJson, file);
      file.append(",\n");
    }
  }

  private static JSONArray convertSuggestionToJson(Collection<Suggestion> suggestions) {
    JSONArray array = new JSONArray();
    for (Suggestion sug : suggestions) {
      JSONObject temp = new JSONObject(new LinkedHashMap());
      temp.put("change_type", sug.getChangeType().label);
      temp.put("entity_type", sug.getEntityType().label);
      temp.put("identifier", sug.getIdentifier());
      temp.put("confidence", sug.getConfidence());
      array.add(temp);
    }
    return array;
  }

  private static JSONObject compare(
      String message, Set<Suggestion> groundTruth, Set<Suggestion> suggestions) {

    List<Suggestion> suggestionList = new ArrayList<>(suggestions);
    JSONObject json = new JSONObject();

    json.put("ground_truth", convertSuggestionToJson(groundTruth));
    json.put("suggestion", convertSuggestionToJson(suggestions));

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
    double precision = computeMetric(correctNum, outputNum);
    double recall = computeMetric(correctNum, groundTruthNum);
    System.out.print(message + ": " + "Precision=" + precision + " Recall=" + recall + " ");

    json.put("precision", precision);
    json.put("recall", recall);

    // order considered: MAP
    double averagePrecision = 0D;
    double correctForK = 0D;
    double recallForPreviousK = 0D;
    for (int k = 1; k <= outputNum; k++) {
      if (hitGroundTruth(groundTruth, suggestionList.get(k - 1))) {
        correctForK += 1;
      }
      double precisionForK = correctForK / k;
      double recallForK = correctForK / groundTruthNum;
      averagePrecision += precisionForK * (recallForK - recallForPreviousK);
      recallForPreviousK = recallForK;
    }
    averagePrecision = MetricUtil.formatDouble(averagePrecision * 100);
    System.out.println("Average Precision=" + averagePrecision);
    json.put("average_precision", averagePrecision);

    return json;
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
        if (gt.getIdentifier().equals(suggestion.getIdentifier())
            || gt.getIdentifier().endsWith(suggestion.getIdentifier())) {
          return true;
        }
      }
    }
    return false;
  }

  private static double computeMetric(int a, int b) {
    return b == 0 ? 0D : MetricUtil.formatDouble((double) a * 100 / b);
  }

  private static void generateSuggestion(Map<String, Binding> bindingInfos) {
    for (Map.Entry<String, Binding> entry : bindingInfos.entrySet()) {
      String id = entry.getKey();
      Binding binding = entry.getValue();
      for (var temp : binding.getFiles().entrySet()) {
        mergeOutputEntry(
            suggestedFiles,
            new Suggestion(ChangeType.UPDATED, EntityType.FILE, temp.getKey(), temp.getValue()));
      }
      for (var temp : binding.getTypes().entrySet()) {
        mergeOutputEntry(
            suggestedTypes,
            new Suggestion(ChangeType.UPDATED, EntityType.TYPE, temp.getKey(), temp.getValue()));
      }
      for (var temp : binding.getMembers().entrySet()) {
        mergeOutputEntry(
            suggestedMembers,
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

  private static void mergeOutputEntry(Set<Suggestion> output, Suggestion suggestion) {
    if (suggestion.getIdentifier().isEmpty() || suggestion.getIdentifier().isBlank()) {
      output.add(suggestion);
    } else {
      // if exist, change confidence
      boolean exist = false;

      for (Suggestion ot : output) {
        if (ot.getEntityType().equals(suggestion.getEntityType())
            && ot.getIdentifier().equals(suggestion.getIdentifier())) {
          ot.setConfidence(ot.getConfidence() + suggestion.getConfidence());
          exist = true;
          break;
        }
      }
      // else, add a new
      if (!exist) {
        output.add(suggestion);
      }
    }
  }

  private static void mergeOutputEntry(Set<Suggestion> output, Set<Suggestion> suggestions) {
    for (var sug : suggestions) {
      mergeOutputEntry(output, sug);
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

    Counter<ElementNode> counter = new Counter<>();
    for (Edge useEdge : useEdges) {
      Node refNode = graph.getEdgeSource(useEdge);
      if (refNode.getLanguage().equals(Language.JAVA)) {
        // find wrapped member, type, and file nodes, return names
        Triple<String, String, String> entities = findWrappedEntities(graph, refNode);
        // filter incorrect references with incorrect view
        binding.addRefEntities(entities);
        //        relevantNodes.add(refNode);
        for (ElementNode n : getIndirectNodes(graph, refNode)) {
          counter.add(n, 1);
        }
      }
    }
    for (ElementNode rNode : counter.mostCommon(5)) {
      Triple<String, String, String> entities = findWrappedEntities(graph, rNode);
      binding.addRefEntities(entities);
    }
    return binding;
  }

  /**
   * Get all connected nodes (k hops or dynamic hops)
   *
   * @param graph
   * @param node
   * @return
   */
  private static Set<ElementNode> getIndirectNodes(Graph<Node, Edge> graph, Node node) {
    Set<ElementNode> results = new HashSet<>();
    // intersect the 2-hop nodes
    Set<Edge> useEdges = getUseEdges(graph, node);

    for (Edge useEdge : useEdges) {
      Node refNode = graph.getEdgeSource(useEdge);
      if (refNode.getLanguage().equals(Language.JAVA) && refNode instanceof ElementNode) {
        results.add((ElementNode) refNode);
      }
    }

    // TODO: determine by dynamical hop by access
    //    Optional<Object> accessOpt = node.getAttribute("access");
    //    if (accessOpt.isPresent()) {
    //    switch (access) {
    //      case "private":
    //        // find all indirect refs under the same type
    //        break;
    //      default:
    //    }
    //    }
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
      Counter<String> filesCounter = new Counter<>();
      Counter<String> typesCounter = new Counter<>();
      Counter<String> membersCounter = new Counter<>();

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
            filesCounter.add(diffFilePath, 1);
          }
          for (JavaDiff diff : diffs) {
            String diffType = diff.getType();
            if (!diffType.isEmpty()) {
              typesCounter.add(diffType, 1);
            }

            String diffMember = diff.getMember();
            if (!diffMember.isEmpty()) {
              membersCounter.add(diffMember, 1);
            }
          }
        }
      }
      // store and consume
      // compute confidence for each entity
      List<String> freqFiles = filesCounter.mostCommon(5);
      List<String> freqTypes = typesCounter.mostCommon(5);
      List<String> freqMembers = membersCounter.mostCommon(5);
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
          String rootSrcFolder =
              path.substring(
                  0, path.toLowerCase().indexOf(convertQNameToPath(packageName).toLowerCase()));
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
