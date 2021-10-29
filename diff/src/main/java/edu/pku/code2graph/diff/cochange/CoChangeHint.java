package edu.pku.code2graph.diff.cochange;

import com.google.gson.Gson;
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
import edu.pku.code2graph.util.GraphUtil;
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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class CoChangeHint {
  static Logger logger = LoggerFactory.getLogger(CoChangeHint.class);

  private static final String rootFolder = Config.rootDir;
  private static String repoName = "";
  private static String repoPath = "";
  private static final String tempDir = Config.tempDir;
  private static final String outputDir = Config.outputDir;

  // precisions and recalls for each commit
  private static List<Double> filePs = new ArrayList<>();
  private static List<Double> fileRs = new ArrayList<>();
  private static List<Double> typePs = new ArrayList<>();
  private static List<Double> typeRs = new ArrayList<>();
  private static List<Double> memberPs = new ArrayList<>();
  private static List<Double> memberRs = new ArrayList<>();

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

    List<String> focusList = new ArrayList<>();
    //    focusList.add("111a0f9f171341f2c35f1c10cdddcb9dcf53f405");

    //    BasicConfigurator.configure();
    PropertyConfigurator.configure(
        System.getProperty("user.dir") + File.separator + "log4j.properties");
    repoName = Config.repoName;
    repoPath = Config.repoPath;
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    GitService gitService = new GitServiceCGit(repoPath);

    System.out.printf("Processing repo: %s at %s %n", repoName, repoPath);
    List<String> dataFilePaths =
        FileUtil.listFilePaths(tempDir + File.separator + repoName, ".json");

    if (focusList.isEmpty()) {
      FileUtil.clearDir(outputDir + File.separator + repoName);
    }

    filePs = new ArrayList<>();
    fileRs = new ArrayList<>();
    typePs = new ArrayList<>();
    typeRs = new ArrayList<>();
    memberPs = new ArrayList<>();
    memberRs = new ArrayList<>();

    Gson gson = new Gson();

    // one file, one commit
    for (String dataFilePath : dataFilePaths) {
      String commitID = FileUtil.getFileNameFromPath(dataFilePath).replace(".json", "");

      if (!focusList.isEmpty()) {
        if (!focusList.contains(commitID)) {
          continue;
        }
      }

      SortedSet<Suggestion> suggestedFiles = new TreeSet<>(new SuggestionComparator());
      SortedSet<Suggestion> suggestedTypes = new TreeSet<>(new SuggestionComparator());
      SortedSet<Suggestion> suggestedMembers = new TreeSet<>(new SuggestionComparator());
      suggestedFiles.clear();
      suggestedTypes.clear();
      suggestedMembers.clear();

      // Input: XMLDiff (file relative path: <file relative path, changed xml element type, id>)
      Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
      // Ground Truth: JavaDiff (file relative path: <file relative path, type name, member
      // name>)
      Map<String, List<JavaDiff>> javaDiffs = new HashMap<>();
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

      System.out.printf(
          "XML diff files: %d for commit: %s %n", xmlDiffs.entrySet().size(), commitID);
      System.out.printf(
          "Java diff files: %d for commit: %s %n", javaDiffs.entrySet().size(), commitID);

      // restore to master or there will be strangle bugs
      logger.info(
          SysUtil.runSystemCommand(
              repoPath, Charset.defaultCharset(), "git", "checkout", "-f", "master"));

      // checkout to the previous version
      logger.info(
          SysUtil.runSystemCommand(
              repoPath,
              Charset.defaultCharset(),
              "git",
              "checkout", /* "-b","changelint", */
              "-f",
              commitID + "~"));

      String parentCommitID =
          SysUtil.runSystemCommand(
              repoPath, Charset.defaultCharset(), "git", "log", "--pretty=%P", "-n", "1", commitID);

      logger.info(
          "Now at HEAD commit: {}Expected at commit: {}",
          SysUtil.runSystemCommand(repoPath, Charset.defaultCharset(), "git", "rev-parse", "HEAD"),
          parentCommitID);

      System.out.println("Building graph...");

      //   build the graph for the current version
      GraphUtil
          .clearGraph(); // !must clear the static graph caches, or the graph will keep growing!
      Graph<Node, Edge> graph = buildGraph();

      System.out.printf(
          "Graph building done, nodes: %d; edges: %d %n",
          graph.vertexSet().size(), graph.edgeSet().size());

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

        //        HistoricalCochange historicalCochange =
        //            computeHistoricalCochanges(repoAnalyzer, gitService, path);
        // set empty for ablation study
        HistoricalCochange historicalCochange = new HistoricalCochange();

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

              generateSuggestion(bindingInfos, suggestedFiles, suggestedTypes, suggestedMembers);
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
              generateSuggestion(
                  bindingInfos,
                  historicalCochange,
                  contextNodes,
                  suggestedFiles,
                  suggestedTypes,
                  suggestedMembers);
            }
          }
        }
      }
      // measure accuracy by comparing with ground truth
      evaluate(commitID, javaDiffs, suggestedFiles, suggestedTypes, suggestedMembers);
    }

    System.out.println("File: ");
    System.out.println("P=" + MetricUtil.getMean(filePs));
    System.out.println("R=" + MetricUtil.getMean(fileRs));
    System.out.println("Type: ");
    System.out.println("P=" + MetricUtil.getMean(typePs));
    System.out.println("R=" + MetricUtil.getMean(typeRs));
    System.out.println("Member: ");
    System.out.println("P=" + MetricUtil.getMean(memberPs));
    System.out.println("R=" + MetricUtil.getMean(memberRs));
  }

  private static void evaluate(
      String commitID,
      Map<String, List<JavaDiff>> groundTruth,
      SortedSet<Suggestion> suggestedFiles,
      SortedSet<Suggestion> suggestedTypes,
      SortedSet<Suggestion> suggestedMembers)
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

    String outputPath = outputDir + File.separator + repoName + File.separator + commitID + ".json";

    File outputFile = new File(outputPath);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    JSONObject outputJson = new JSONObject(new LinkedHashMap());

    outputJson.put("commit_id", commitID);

    // separately on three levels
    System.out.println("For commit: " + commitID);
    JSONObject fileResult = compare("File level", gtAllFiles, suggestedFiles);
    JSONObject typeResult = compare("Type level", gtAllTypes, suggestedTypes);
    JSONObject memberResult = compare("Member level", gtAllMembers, suggestedMembers);

    filePs.add((Double) fileResult.get("precision"));
    fileRs.add((Double) fileResult.get("recall"));
    typePs.add((Double) typeResult.get("precision"));
    typeRs.add((Double) typeResult.get("recall"));
    memberPs.add((Double) memberResult.get("precision"));
    memberRs.add((Double) memberResult.get("recall"));

    outputJson.put("file", fileResult);
    outputJson.put("type", typeResult);
    outputJson.put("member", memberResult);

    try (FileWriter file = new FileWriter(outputPath, false)) {
      JSONObject.writeJSONString(outputJson, file);
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

    // precision and recall
    double precision = MetricUtil.computeProportion(correctNum, outputNum);
    double recall = MetricUtil.computeProportion(correctNum, groundTruthNum);
    System.out.print(message + ": " + "Precision=" + precision + " Recall=" + recall + " ");
    System.out.println();

    json.put("precision", precision);
    json.put("recall", recall);
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
      // for added, identifier cannot be precisely predicted, but here we strictly requires it
      for (Suggestion gt : groundTruth) {
        if (gt.getIdentifier().equals(suggestion.getIdentifier())
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

  /**
   * Generate co-change suggestion for newly added code
   *
   * @param bindingInfos
   * @param suggestedFiles
   * @param suggestedTypes
   * @param suggestedMembers
   */
  private static void generateSuggestion(
      Map<String, Binding> bindingInfos,
      SortedSet<Suggestion> suggestedFiles,
      SortedSet<Suggestion> suggestedTypes,
      SortedSet<Suggestion> suggestedMembers) {
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
   * Generate co-change suggestion for modified/deleted code
   *
   * @param bindingInfos
   * @param historicalCochange
   * @param contextNodes
   * @param suggestedFiles
   * @param suggestedTypes
   * @param suggestedMembers
   */
  private static void generateSuggestion(
      Map<String, Binding> bindingInfos,
      HistoricalCochange historicalCochange,
      Map<String, Double> contextNodes,
      SortedSet<Suggestion> suggestedFiles,
      SortedSet<Suggestion> suggestedTypes,
      SortedSet<Suggestion> suggestedMembers) {
    Map<String, Map<String, Double>> fileLookup = new HashMap<>();
    Map<String, Map<String, Double>> typeLookup = new HashMap<>();
    Map<String, Map<String, Double>> memberLookup = new HashMap<>();

    for (Map.Entry<String, Binding> entry : bindingInfos.entrySet()) {
      String id = entry.getKey();
      Binding binding = entry.getValue();
      buildLookup(fileLookup, id, binding.getFiles());
      buildLookup(typeLookup, id, binding.getTypes());
      buildLookup(memberLookup, id, binding.getMembers());
    }

    for (var hisEntry : historicalCochange.members.entrySet()) {
      if (memberLookup.containsKey(hisEntry.getKey())) {
        Map<String, Double> row = memberLookup.get(hisEntry.getKey());
        // update confidence of each item
        for (var rowEntry : row.entrySet()) {
          double updatedConfidence = rowEntry.getValue() * (1 + hisEntry.getValue());
          row.put(rowEntry.getKey(), updatedConfidence);
        }
      }
      //      } else {
      //        Map<String, Double> newRow = new LinkedHashMap<>();
      //        for (String s : contextNodes.keySet()) {
      //          newRow.put(s, hisEntry.getValue());
      //        }
      //        memberLookup.put(hisEntry.getKey(), newRow);
      //      }
    }

    mergeOutputEntry(
        suggestedFiles, collaborativeFilter(EntityType.FILE, fileLookup, contextNodes));
    mergeOutputEntry(
        suggestedTypes, collaborativeFilter(EntityType.TYPE, typeLookup, contextNodes));
    mergeOutputEntry(
        suggestedMembers, collaborativeFilter(EntityType.MEMBER, memberLookup, contextNodes));
  }

  private static void mergeOutputEntry(Set<Suggestion> output, Suggestion suggestion) {
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

  private static void mergeOutputEntry(Set<Suggestion> output, Set<Suggestion> suggestions) {
    for (var sug : suggestions) {
      mergeOutputEntry(output, sug);
    }
  }

  /**
   * Evaluate and rank the co-change probability with neighborhood-based&user-based CF algorithm
   *
   * @param entityType
   * @param lookup
   * @param contextNodes
   * @return
   */
  private static Set<Suggestion> collaborativeFilter(
      EntityType entityType,
      Map<String, Map<String, Double>> lookup,
      Map<String, Double> contextNodes) {
    Set<Suggestion> results = new HashSet<>();

    for (var entityEntry : lookup.entrySet()) {
      Map<String, Double> reverseRefs = entityEntry.getValue();
      double sum1 = 0D;
      double sum2 = 0D;
      for (var id : contextNodes.keySet()) {
        sum1 += (contextNodes.get(id) * reverseRefs.getOrDefault(id, -1D));
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
        results.add(new Suggestion(ChangeType.ADDED, entityType, "", confidence));
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
      Map<String, Map<String, Double>> lookup, String id, Map<String, Integer> refs) {
    for (Map.Entry<String, Integer> entry : refs.entrySet()) {
      if (!lookup.containsKey(entry.getKey())) {
        lookup.put(entry.getKey(), new HashMap<>());
      }
      lookup.get(entry.getKey()).put(id, Double.valueOf(entry.getValue()));
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
        if (scopeFilePaths.isEmpty() || scopeFilePaths.contains(entities.getLeft())) {
          binding.addRefEntities(entities);
          //        relevantNodes.add(refNode);
          for (ElementNode n : getIndirectNodes(graph, refNode)) {
            counter.add(n, 1);
          }
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

  /**
   * Collect evolutionary coupling entities from previous commits that ever changed a file
   *
   * @param repoAnalyzer
   * @param gitService
   * @param path
   * @return
   */
  private static HistoricalCochange computeHistoricalCochanges(
      RepoAnalyzer repoAnalyzer, GitService gitService, String path) {
    System.out.println("Computing historical changes for " + path);

    Counter<String> filesCounter = new Counter<>();
    Counter<Pair<String, String>> typesCounter = new Counter<>();
    Counter<Triple<String, String, String>> membersCounter = new Counter<>();

    // note that here "HEAD" is the tested commit, since we have checkout to it before
    List<String> commitIDs = gitService.getCommitsChangedFile(path, "HEAD", 10);
    int numAllCommits = commitIDs.size();
    // count the number of co-change commits

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
          for (JavaDiff diff : diffs) {
            String diffType = diff.getType();
            if (!diffType.isEmpty()) {
              typesCounter.add(Pair.of(diffFilePath, diffType), 1);
            }

            String diffMember = diff.getMember();
            if (!diffMember.isEmpty()) {
              membersCounter.add(Triple.of(diffFilePath, diffType, diffMember), 1);
            }
          }
        }
      }
    }

    // store and consume
    // compute confidence for each entity
    HistoricalCochange result = new HistoricalCochange();

    for (String k : filesCounter.mostCommon(5)) {
      double confidence =
          filesCounter.get(k) >= numAllCommits ? 1.0 : (double) filesCounter.get(k) / numAllCommits;
      result.files.put(k, MetricUtil.formatDouble(confidence));
    }

    for (Pair<String, String> k : typesCounter.mostCommon(5)) {
      double confidence =
          typesCounter.get(k) >= numAllCommits ? 1.0 : (double) typesCounter.get(k) / numAllCommits;
      result.types.put(k.getRight(), MetricUtil.formatDouble(confidence));
    }

    for (Triple<String, String, String> k : membersCounter.mostCommon(5)) {
      double confidence =
          membersCounter.get(k) >= numAllCommits
              ? 1.0
              : (double) membersCounter.get(k) / numAllCommits;
      result.members.put(k.getRight(), MetricUtil.formatDouble(confidence));
    }
    return result;
  }

  /**
   * Collect evolutionary coupling entities from previous commits that ever changed all examined xml
   * files
   *
   * @param repoAnalyzer
   * @param gitService
   * @param xmlDiffs
   * @return
   */
  private static HistoricalCochange computeHistoricalCochanges(
      RepoAnalyzer repoAnalyzer, GitService gitService, Map<String, List<XMLDiff>> xmlDiffs) {

    Counter<String> filesCounter = new Counter<>();
    Counter<String> typesCounter = new Counter<>();
    Counter<String> membersCounter = new Counter<>();

    // get all commits that ever changed each xml file

    Map<String, Integer> commitCounter = new HashMap<>();
    for (String xmlFilePath : xmlDiffs.keySet()) {
      // note that here "HEAD" is the tested commit, since we have checkout to it before
      List<String> commitIDs = gitService.getCommitsChangedFile(xmlFilePath, "HEAD", 10);
      for (String commitID : commitIDs) {
        commitCounter.merge(commitID, 1, Integer::sum);
      }
    }

    // get commits that modified all xml files at the same time
    List<String> commitIDs = new ArrayList<>();
    for (var entry : commitCounter.entrySet()) {
      if (entry.getValue() == xmlDiffs.size()) {
        commitIDs.add(entry.getKey());
      }
    }

    int numAllCommits = commitIDs.size();
    // count the number of co-change commits

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
    HistoricalCochange result = new HistoricalCochange();

    for (String k : filesCounter.mostCommon(5)) {
      double confidence =
          filesCounter.get(k) >= numAllCommits ? 1.0 : (double) filesCounter.get(k) / numAllCommits;
      result.files.put(k, MetricUtil.formatDouble(confidence));
    }

    for (String k : typesCounter.mostCommon(5)) {
      double confidence =
          typesCounter.get(k) >= numAllCommits ? 1.0 : (double) typesCounter.get(k) / numAllCommits;
      result.types.put(k, MetricUtil.formatDouble(confidence));
    }

    for (String k : membersCounter.mostCommon(5)) {
      double confidence =
          membersCounter.get(k) >= numAllCommits
              ? 1.0
              : (double) membersCounter.get(k) / numAllCommits;
      result.members.put(k, MetricUtil.formatDouble(confidence));
    }
    return result;
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

    System.out.println("Java files: " + filePaths.size());

    // collect all xml layout files
    Set<String> xmlFilePaths =
        FileUtil.listFilePaths(repoPath, ".xml").stream()
            .filter(path -> path.contains("layout") || path.contains("menu"))
            .collect(Collectors.toSet());

    System.out.println("XML files: " + xmlFilePaths.size());

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
