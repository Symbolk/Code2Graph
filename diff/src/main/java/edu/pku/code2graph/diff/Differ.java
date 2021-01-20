package edu.pku.code2graph.diff;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.DiffHunk;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Outermost API provider for diff module */
public class Differ {

  Logger logger = LoggerFactory.getLogger(Differ.class);

  // meta info
  private String repoName;
  private String repoPath;
  private String tempPath; // specifically for this repo

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
  public void computeDiff() throws IOException {

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

    Graph<Node, Edge> aGraph = generator.generateFromFiles(tempFilePaths.getLeft());
    Graph<Node, Edge> bGraph = generator.generateFromFiles(tempFilePaths.getRight());

    // 2. compare graphs and compute diffs
    compare(aGraph, bGraph);
    // return graph-patch
  }

  /**
   * Compare a specific commit with its parent
   *
   * @param commitID
   */
  public void computeDiff(String commitID) throws IOException {
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
    Generators generator = Generators.getInstance();

    Graph<Node, Edge> aGraph = generator.generateFromFiles(tempFilePaths.getLeft());
    Graph<Node, Edge> bGraph = generator.generateFromFiles(tempFilePaths.getRight());

    // 2. compare graphs and compute diffs
    compare(aGraph, bGraph);
    // return graph-patch

  }

  private void compare(Graph<Node, Edge> aGraph, Graph<Node, Edge> bGraph) {}
}
