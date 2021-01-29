package edu.pku.code2graph.diff;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.DiffHunk;
import edu.pku.code2graph.diff.model.Mapping;
import edu.pku.code2graph.diff.model.Version;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.io.ObjectExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Outermost API provider for diff module */
public class Differ {
  Logger logger = LoggerFactory.getLogger(Differ.class);

  // meta info
  private String repoName;
  private String repoPath;
  private String tempPath; // specifically for this repo+
  private Graph<Node, Edge> aGraph;
  private Graph<Node, Edge> bGraph;

  // options

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
    List<DiffFile> allDiffFiles = repoAnalyzer.analyzeWorkingTree();
    List<DiffHunk> allDiffHunks = repoAnalyzer.getDiffHunks();

    if (allDiffFiles.isEmpty()) {
      logger.warn("No changes, working tree clean.");
      return;
    }
    if (allDiffHunks.isEmpty()) {
      logger.warn("Changes exist, but not in file contents.");
      return;
    }

    DataCollector dataCollector = new DataCollector(tempPath);

    Pair<List<String>, List<String>> tempFilePaths =
        dataCollector.collectForWorkingTree(allDiffFiles);
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
    List<DiffFile> allDiffFiles = repoAnalyzer.analyzeCommit(commitID);
    List<DiffHunk> allDiffHunks = repoAnalyzer.getDiffHunks();

    if (allDiffFiles.isEmpty() || allDiffHunks.isEmpty()) {
      logger.info("No changes at commit: " + commitID);
      return;
    }

    DataCollector dataCollector = new DataCollector(tempPath);
    Pair<List<String>, List<String>> tempFilePaths =
        dataCollector.collectForCommit(commitID, allDiffFiles);
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
        aGraph,
        tempPath + File.separator + commitID + File.separator + Version.A.asString() + ".dat");
    GraphUtil.clearGraph();

    bGraph = generator.generateFromFiles(tempFilePaths.getRight());
  }

  /**
   * Compare graphs and compute diffs
   *
   * @return return graph-patch
   */
  public void compareGraphs() {
    // top down filtering
    Mapping mapping = topDownMatch();

    System.out.println(mapping);
    // bottom up matching

    // actually: find&filter matching, and leave others as edits

    // graph-patch in the form of diff nodes (remaining unmatched nodes)

  }

  /**
   * Top down matching/filtering/pruning by hash signature of type and qname for element nodes
   *
   * @return
   */
  private Mapping topDownMatch() {
    // filter element nodes by hash signature of type and qname
    Mapping mapping = new Mapping();
    Set<ElementNode> elementNodeSet1 =
        aGraph.vertexSet().stream()
            .filter(node -> (node instanceof ElementNode))
            .map(ElementNode.class::cast)
            .collect(Collectors.toSet());
    Set<ElementNode> elementNodeSet2 =
        bGraph.vertexSet().stream()
            .filter(node -> (node instanceof ElementNode))
            .map(ElementNode.class::cast)
            .collect(Collectors.toSet());
    Map<Integer, ElementNode> signMap1 =
        elementNodeSet1.stream()
            .collect(
                Collectors.toMap(
                    ElementNode::hashCode, Function.identity(), (o, n) -> o, HashMap::new));
    Map<Integer, ElementNode> signMap2 =
        elementNodeSet2.stream()
            .collect(
                Collectors.toMap(
                    ElementNode::hashCode, Function.identity(), (o, n) -> o, HashMap::new));
    for (Map.Entry<Integer, ElementNode> entry : signMap1.entrySet()) {
      if (signMap2.containsKey(entry.getKey())) {
        // add the matched nodes into the matching relationships
        mapping.one2one.put(entry.getValue(), signMap2.get(entry.getKey()));
        // remove the mapped node from other
        signMap2.remove(entry.getKey());
      } else {
        mapping.addUnmatched1(entry.getValue());
      }
    }
    signMap2.entrySet().forEach(entry -> mapping.addUnmatched2(entry.getValue()));

    // filter relation nodes with textual diff


    return mapping;
  }
}
