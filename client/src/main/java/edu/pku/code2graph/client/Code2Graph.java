package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.Differ;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.XLLDetector;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Java API client */
public class Code2Graph {
  // meta info
  private final String repoName;
  private final String repoPath;
  private String tempDir;

  // components
  private Generators generator;
  private Differ differ;

  // options
  private boolean involveImported = false; // consider files imported by diff files or not
  private boolean limitToSource = true; // limit the node inside collected source files or not

  public Code2Graph(String repoName, String repoPath) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.differ = new Differ(repoName, repoPath);
    this.generator = Generators.getInstance();
  }

  public Code2Graph(String repoName, String repoPath, String tempDir) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.differ = new Differ(repoName, repoPath, tempDir);
    this.generator = Generators.getInstance();
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

  public String getRepoName() {
    return repoName;
  }

  public String getRepoPath() {
    return repoPath;
  }

  /** Compare the old (A) and new (B) version graphs of the working tree */
  public void compareGraphs() {
    try {
      differ.buildGraphs();
      differ.compareGraphs();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Compare the old (A) and new (B) version graphs of a commit
   *
   * @param commitID
   */
  public void compareGraphs(String commitID) {
    if (commitID.isEmpty()) {
      // TODO check commit id validity
      System.out.println("Invalid commit id: " + commitID);
    }
    try {
      differ.buildGraphs(commitID);
      differ.compareGraphs();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Construct graph from a code repository
   *
   * @param repoPath
   * @return
   */
  public Graph<Node, Edge> generateGraph(String repoPath) {
    // collect the path list of source files in supported languages
    return generateGraph(new ArrayList<>());
  }

  /**
   * Construct graph from a list of source files
   *
   * @param filePaths
   * @return
   */
  public Graph<Node, Edge> generateGraph(List<String> filePaths) {
    try {
      // construct graph with intra-language nodes and edges
      Graph<Node, Edge> graph = generator.generateFromFiles(filePaths);
      // build cross-language linking (XLL) edges
      List<Pair<URI, URI>> links = XLLDetector.detect(GraphUtil.getUriSets());

      for (Pair<URI, URI> link : links) {
        System.out.println(link);
        // get nodes by URI

        // create XLL edge
        //              graph.addEdge(
        //                  triple.getFirst(), node, new Edge(GraphUtil.eid(), triple.getSecond()))
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return GraphUtil.getGraph();
  }
}
