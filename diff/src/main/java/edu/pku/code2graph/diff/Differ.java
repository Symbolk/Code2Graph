package edu.pku.code2graph.diff;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.DiffHunk;
import edu.pku.code2graph.diff.model.Mapping;
import edu.pku.code2graph.diff.model.Version;
import edu.pku.code2graph.diff.util.MetricUtil;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.io.ObjectExporter;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.pku.code2graph.model.TypeSet.type;

/** Outermost API provider for diff module */
public class Differ {
  Logger logger = LoggerFactory.getLogger(Differ.class);
  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

  // meta info
  private String repoName;
  private String repoPath;
  private String tempPath; // specifically for this repo

  // diff info
  private List<DiffFile> diffFiles = new ArrayList<>();
  private List<DiffHunk> diffHunks = new ArrayList<>();
  private Graph<Node, Edge> bGraph;
  private Graph<Node, Edge> aGraph;
  private Mapping mapping = new Mapping();

  // options
  private double threshold = 0.75D;

  public Differ(String repoName, String repoPath) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.tempPath = System.getProperty("java.io.tmpdir") + File.separator + repoName;
  }

  public Differ(String repoName, String repoPath, String tempDir) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.tempPath = tempDir + File.separator + repoName;
  }

  static {
    initGenerators();
  }

  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  public static void initGenerators() {
    ClassIndex.getSubclasses(Generator.class)
        .forEach(
            gen -> {
              Register a = gen.getAnnotation(Register.class);
              if (a != null) Generators.getInstance().install(gen, a);
            });
  }

  /** Analyze changes in working directory */
  public void buildGraphs() throws IOException {

    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    diffFiles = repoAnalyzer.analyzeWorkingTree();
    diffHunks = repoAnalyzer.getDiffHunks();

    if (diffFiles.isEmpty()) {
      logger.warn("No changes, working tree clean.");
      return;
    }
    if (diffHunks.isEmpty()) {
      logger.warn("Changes exist, but not in file contents.");
      return;
    }

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    tempPath = tempPath + sdf.format(timestamp) + File.separator;
    DataCollector dataCollector = new DataCollector(tempPath);

    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);
    // 1. generate 2 graphs for working tree: 2 graphs: left and right
    Generators generator = Generators.getInstance();

    aGraph = generator.generateFromFiles(tempFilePaths.getLeft());
    // save into file and read when compare
    // clear global graph
    bGraph = generator.generateFromFiles(tempFilePaths.getRight());
  }

  /**
   * Compare a specific commit with its parent
   *
   * @param commitID
   */
  public void buildGraphs(String commitID) throws IOException {
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    diffFiles = repoAnalyzer.analyzeCommit(commitID);
    diffHunks = repoAnalyzer.getDiffHunks();

    if (diffFiles.isEmpty() || diffHunks.isEmpty()) {
      logger.info("No changes at commit: " + commitID);
      return;
    }

    tempPath = tempPath + File.separator + commitID;
    DataCollector dataCollector = new DataCollector(tempPath);
    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);
    // 1. generate 2 graphs for working tree: 2 graphs: left and right

    // TODO use two threads to generate graphs in parallel
    //    ExecutorService executorService = Executors.newFixedThreadPool(2);
    //    Future<Graph<Node, Edge>> task1 = executorService.submit(generator1);
    //    Future<Graph<Node, Edge>> task2 = executorService.submit(generator2);
    //    Graph<Node, Edge> aGraph = task1.get();
    //    Graph<Node, Edge> bGraph = task2.get();
    //    executorService.shutdown();

    Generators generator = Generators.getInstance();

    aGraph = generator.generateFromFiles(tempFilePaths.getLeft());

    ObjectExporter.exportObjectToFile(
        aGraph, tempPath + File.separator + Version.A.asString() + ".dat");
    GraphUtil.clearGraph();

    bGraph = generator.generateFromFiles(tempFilePaths.getRight());
  }

  /**
   * Compare graphs and compute diffs
   *
   * @return return graph-patch
   */
  public void compareGraphs() {
    // init nodes to mappings by type
    //    initMapping();
    // top down filtering
    matchBySignature();

    // filter nodes with textual diff (coarsely)
    filterWithDiffHunks(Version.A);
    filterWithDiffHunks(Version.B);

    // bottom up matching
    alignByContext();

    // the diff output: graph-patch format
    generateGraphPatch();
  }

  /** Generate graph patch from mapping */
  private void generateGraphPatch() {
    // how to preserve edges between unmatched nodes

  }

  //  private void initMapping() {
  //    aGraph.vertexSet().stream().forEach(node -> mapping.addUnmatched1(node));
  //    bGraph.vertexSet().stream().forEach(node -> mapping.addUnmatched2(node));
  //  }

  /**
   * Top down to match/filter/prune unchanged nodes and get unmatched nodes
   *
   * @return
   */
  private void matchBySignature() {
    // filter element nodes by hash signature of type and qname
    // filter relation nodes by hash signature of type and snippet
    Map<Integer, Node> signMap1 =
        aGraph.vertexSet().stream()
            .collect(
                Collectors.toMap(
                    Node::hashSignature, Function.identity(), (o, n) -> o, HashMap::new));
    Map<Integer, Node> signMap2 =
        bGraph.vertexSet().stream()
            .collect(
                Collectors.toMap(
                    Node::hashSignature, Function.identity(), (o, n) -> o, HashMap::new));
    for (Map.Entry<Integer, Node> entry : signMap1.entrySet()) {
      if (signMap2.containsKey(entry.getKey())) {
        // add the matched nodes into the matching relationships
        mapping.addToMatched(entry.getValue(), signMap2.get(entry.getKey()));
        // remove the mapped node from other
        signMap2.remove(entry.getKey());
      } else {
        mapping.addToUnmatched1(entry.getValue());
      }
    }
    signMap2.forEach((key, value) -> mapping.addToUnmatched2(value));
  }

  /** Bottom up match to further prune unmatched nodes to get diff nodes */
  private void alignByContext() {
    // divide and conquer by node type
    // basic assumption: only nodes with the same type can be matched

    for (Map.Entry<Type, Set<ElementNode>> entry : mapping.getUnmatchedElementNodes1().entrySet()) {
      Set<ElementNode> nodes1 = entry.getValue();
      Type type = entry.getKey();
      if (mapping.getUnmatchedElementNodes2().containsKey(type)) {
        Set<ElementNode> nodes2 = mapping.getUnmatchedElementNodes2().get(type);
        matchElementNodes(nodes1, nodes2);
      }
    }
    for (Map.Entry<Type, Set<RelationNode>> entry :
        mapping.getUnmatchedRelationNodes1().entrySet()) {
      Set<RelationNode> nodes1 = entry.getValue();
      Type type = entry.getKey();
      if (mapping.getUnmatchedElementNodes2().containsKey(type)) {
        Set<RelationNode> nodes2 = mapping.getUnmatchedRelationNodes2().get(type);
        matchRelationNodes(nodes1, nodes2);
      }
    }
  }

  /**
   * Match nodes of the same type with bipartite matching
   *
   * @param nodes1
   * @param nodes2
   */
  public void matchElementNodes(Set<ElementNode> nodes1, Set<ElementNode> nodes2) {
    // use bipartite to match methods according to similarity
    Set<ElementNode> partition1 = new HashSet<>();
    Set<ElementNode> partition2 = new HashSet<>();
    // should be simple graph: no self-loops and no multiple edges
    DefaultUndirectedWeightedGraph<ElementNode, DefaultWeightedEdge> bipartite =
        new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

    for (ElementNode n1 : nodes1) {
      for (ElementNode n2 : nodes2) {
        double weight = MetricUtil.weightElement(n1, n2);
        if(weight >= threshold) {
        bipartite.addVertex(n1);
          partition1.add(n1);
          bipartite.addVertex(n2);
          partition2.add(n2);
          bipartite.addEdge(n1, n2);
          // compute similarity by aggregating both the self information and the context
          bipartite.setEdgeWeight(n1, n2, weight);
        }
      }
    }
    // bipartite  maximum matching to match most likely matched nodes
    MaximumWeightBipartiteMatching matcher =
        new MaximumWeightBipartiteMatching(bipartite, partition1, partition2);
    Set<DefaultWeightedEdge> edges = matcher.getMatching().getEdges();
    // add one2one found and remove from unmatched
    for (DefaultWeightedEdge edge : edges) {
      Node source = bipartite.getEdgeSource(edge);
      Node target = bipartite.getEdgeTarget(edge);
      double confidence = bipartite.getEdgeWeight(edge);
//      if (confidence >= threshold) {
        mapping.addToMatched(source, target);
        mapping.removeFromUnmatched1(source);
        mapping.removeFromUnmatched2(target);
//      }
    }
  }

  public void matchRelationNodes(Set<RelationNode> nodes1, Set<RelationNode> nodes2) {}

  /**
   * Filter nodes that are not in textual diff ranges
   *
   * @param version
   */
  private void filterWithDiffHunks(Version version) {
    // generate range list as the cache
    Map<String, List<Range>> diffRanges = new HashMap<>();

    Set<ElementNode> fileNodes = new HashSet<>();
    if (version.equals(Version.A)) {
      for (DiffFile diffFile : diffFiles) {
        List<Range> ranges = new ArrayList<>();
        for (DiffHunk diffHunk : diffFile.getDiffHunks()) {
          ranges.add(new Range(diffHunk.getAStartLine(), diffHunk.getAEndLine()));
        }
        diffRanges.put(diffFile.getARelativePath(), ranges);
      }
      fileNodes = mapping.getUnmatchedElementNodes1().get(type("file"));
    } else {
      for (DiffFile diffFile : diffFiles) {
        List<Range> ranges = new ArrayList<>();
        for (DiffHunk diffHunk : diffFile.getDiffHunks()) {
          ranges.add(new Range(diffHunk.getBStartLine(), diffHunk.getBEndLine()));
        }
        diffRanges.put(diffFile.getBRelativePath(), ranges);
      }
      fileNodes = mapping.getUnmatchedElementNodes2().get(type("file"));
    }
    // filter nodes accordingly in a and b version
    for (Node fileNode : fileNodes) {
      String relativePath =
          FileUtil.getRelativePath(
              tempPath + File.separator + version.asString(),
              ((ElementNode) fileNode).getQualifiedName());
      List<Range> diffRange = diffRanges.get(relativePath);
      filterChildren(fileNode, diffRange, version);
    }
  }

  /**
   * Iteratively filter children of nodes
   *
   * @param node
   * @param diffRange
   * @param version
   */
  private void filterChildren(Node node, List<Range> diffRange, Version version) {
    Graph<Node, Edge> graph = version.equals(Version.A) ? aGraph : bGraph;
    List<Node> children = new ArrayList<>();
    graph.outgoingEdgesOf(node).stream()
        .filter(edge -> "child".equals(edge.getType().name))
        .forEach(edge -> children.add(graph.getEdgeTarget(edge)));
    if (children.isEmpty()) {
      return;
    }
    for (Node child : children) {
      boolean overlaps = false;
      for (Range range : diffRange) {
        if (range.overlapsWith(child.getRange())) {
          overlaps = true;
          break;
        }
      }
      if (!overlaps) {
        // if not overlap, remove from unmatched and continue
        if (version.equals(Version.A)) {
          mapping.removeFromUnmatched1(child);
        } else {
          mapping.removeFromUnmatched1(child);
        }
        continue;
      } else {
        // if overlap, iterate to next level children
        filterChildren(child, diffRange, version);
      }
    }
  }

  public Graph<Node, Edge> getbGraph() {
    return bGraph;
  }

  public Graph<Node, Edge> getaGraph() {
    return aGraph;
  }
}
