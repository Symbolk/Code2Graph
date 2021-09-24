package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.Differ;
import edu.pku.code2graph.diff.model.ChangeType;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Detector;
import edu.pku.code2graph.xll.Link;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static edu.pku.code2graph.model.TypeSet.type;

/** Java API client */
public class Code2Graph {
  private static Logger logger = LoggerFactory.getLogger(Code2Graph.class);

  // meta info
  private final String repoName;
  private final String repoPath;
  private final String configPath; // path of the xll configuration
  private String tempDir;

  private Graph<Node, Edge> graph;
  private List<Link> xllLinks;

  // components
  private Generators generator;
  private Differ differ;

  // options
  private Set<Language> supportedLanguages;
  private boolean involveImported = false; // consider files imported by diff files or not
  private boolean limitToSource = true; // limit the node inside collected source files or not

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

  {
    this.generator = Generators.getInstance();
    this.graph = GraphUtil.initGraph();
    this.xllLinks = new ArrayList<>();
    this.supportedLanguages = new HashSet<>();
  }

  public Code2Graph(String repoName, String repoPath, String configPath) {
    FileUtil.setRootPath(repoPath);
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.configPath = configPath;
    this.differ = new Differ(repoName, repoPath);
  }

  public Code2Graph(String repoName, String repoPath, String configPath, String tempDir) {
    FileUtil.setRootPath(repoPath);
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.configPath = configPath;
    this.differ = new Differ(repoName, repoPath, tempDir);
  }

  public void setSupportedLanguages(Set<Language> supportedLanguages) {
    this.supportedLanguages = supportedLanguages;
  }

  public void addSupportedLanguage(Language supportedLanguage) {
    this.supportedLanguages.add(supportedLanguage);
  }

  public Set<Language> getSupportedLanguages() {
    return supportedLanguages;
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

  public List<Link> getXllLinks() {
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
      logger.error("Invalid commit id: " + commitID);
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
    Map<String, List<String>> ext2FilePaths =
        FileUtil.listFilePathsInLanguages(this.repoPath, this.supportedLanguages);
    return generateGraph(ext2FilePaths);
  }

  /**
   * Construct graph from a source file directory
   *
   * @param directory
   * @return
   */
  public Graph<Node, Edge> generateGraph(String directory) {
    // collect the path list of source files in supported languages
    Map<String, List<String>> ext2FilePaths =
        FileUtil.listFilePathsInLanguages(directory, this.supportedLanguages);
    return generateGraph(ext2FilePaths);
  }

  /**
   * Construct graph from a list of source files
   *
   * @param filePaths
   * @return
   */
  public Graph<Node, Edge> generateGraph(List<String> filePaths) {
    if (filePaths.isEmpty()) {
      throw new UnsupportedOperationException("The given file paths are empty");
    } else {
      // a map from generator to file paths
      Map<String, List<String>> filesMap = FileUtil.categorizeFilesByExtension(filePaths);
      return generateGraph(filesMap);
    }
  }

  /**
   * Construct graph from a map from extension to a list of file paths
   *
   * @param ext2FilePaths
   * @return
   */
  public Graph<Node, Edge> generateGraph(Map<String, List<String>> ext2FilePaths) {
    try {
      logger.info("#languages = {}", ext2FilePaths.size());
      for (Map.Entry<String, List<String>> entry : ext2FilePaths.entrySet()) {
        logger.info("- #{} = {}", entry.getKey(), entry.getValue().size());
      }

      // construct graph with intra-language nodes and edges
      logger.info("start building graph");
      Graph<Node, Edge> graph = generator.generateFromFiles(ext2FilePaths);
      logger.info("- #nodes: " + graph.vertexSet().size());
      logger.info("- #edges: " + graph.edgeSet().size());

      // build cross-language linking (XLL) edges
      logger.info("start detecting xll");
      Detector detector = new Detector(GraphUtil.getUriMap(), configPath);
      List<Link> links = detector.linkAll();
      logger.info("- #xll = {}", links.size());
      this.xllLinks = links;
      // create uri-element map when create node
      Map<Language, Map<URI, List<Node>>> uriMap = GraphUtil.getUriMap();
      Type xllType = type("xll");

      int i = 0;
      for (Link link : links) {
        i += 1;
        logger.debug("XLL#{}  {}, {}", i, link.left.toString(), link.right.toString());
        // get nodes by URI
        List<Node> source = uriMap.get(link.left.getLang()).get(link.left);
        List<Node> target = uriMap.get(link.right.getLang()).get(link.right);
        Double weight = 1.0D;

        // create XLL edge
        for (Node left : source) {
          for (Node right : target) {
            graph.addEdge(left, right, new Edge(GraphUtil.eid(), xllType, weight, false, true));
          }
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return GraphUtil.getGraph();
  }

  public Map<Language, Set<URI>> crossLanguageRename(Language lang, URI uri) {
    // def --> uses
    // use --> def --> uses
    // use --> uses

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
