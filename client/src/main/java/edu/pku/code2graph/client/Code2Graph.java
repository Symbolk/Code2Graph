package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.Differ;
import edu.pku.code2graph.diff.model.ChangeType;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.XLLDetector;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;

import java.io.IOException;
import java.util.*;

import static edu.pku.code2graph.model.TypeSet.type;

/** Java API client */
public class Code2Graph {
  // meta info
  private final String repoName;
  private final String repoPath;
  private String tempDir;
  private Graph<Node, Edge> graph;
  private List<Pair<URI, URI>> xllLinks;

  // components
  private Generators generator;
  private Differ differ;

  // options
  private boolean involveImported = false; // consider files imported by diff files or not
  private boolean limitToSource = true; // limit the node inside collected source files or not

  {
    this.generator = Generators.getInstance();
    this.graph = GraphUtil.initGraph();
    this.xllLinks = new ArrayList<>();
  }

  public Code2Graph(String repoName, String repoPath) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.differ = new Differ(repoName, repoPath);
  }

  public Code2Graph(String repoName, String repoPath, String tempDir) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.differ = new Differ(repoName, repoPath, tempDir);
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

  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public List<Pair<URI, URI>> getXllLinks() {
    return xllLinks;
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
   * @return
   */
  public Graph<Node, Edge> generateGraph() {
    // collect the path list of source files in supported languages
    return generateGraph(new ArrayList<>());
  }

  /**
   * Construct graph from a source file directory
   *
   * @param directory
   * @return
   */
  public Graph<Node, Edge> generateGraph(String directory) {
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
      List<Pair<URI, URI>> links = XLLDetector.detect(GraphUtil.getUriMap());
      // create uri-element map when create node
      Map<Language, Map<URI, ElementNode>> uriMap = GraphUtil.getUriMap();
      Type xllType = type("xll");

      for (Pair<URI, URI> link : links) {
        System.out.println(link);
        // get nodes by URI
        Node source = uriMap.get(link.getLeft().getLang()).get(link.getLeft());
        Node target = uriMap.get(link.getLeft().getLang()).get(link.getLeft());
        Double weight = 1.0D;

        // create XLL edge
        graph.addEdge(source, target, new Edge(GraphUtil.eid(), xllType, weight, false, true));
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return GraphUtil.getGraph();
  }

  public Map<Language, Set<URI>> crossLanguageRename(Language lang, URI uri) {
    // traverse xll links

    // return suggested renaming uris
    return new HashMap<>();
  }

  public Map<Language, Set<URI>> crossLanguageCochange(
      Language lang, ChangeType changeType, URI uri) {
    // for deleted: directly find binding uris

    // for added: check xll links of similar/sibling nodes for change suggestion

    return new HashMap<>();
  }

  public Map<Language, Set<URI>> crossLanguageLint(String directory) {
    // check possible abnormal/incomplete xll links
    return new HashMap<>();
  }
}
