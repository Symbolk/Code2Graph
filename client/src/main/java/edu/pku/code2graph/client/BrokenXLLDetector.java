package edu.pku.code2graph.client;

import com.csvreader.CsvWriter;
import edu.pku.code2graph.client.model.BrokenXLL;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.gen.xml.MybatisPreprocesser;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Link;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class BrokenXLLDetector {
  private static Logger logger = LoggerFactory.getLogger(BrokenXLLDetector.class);

  private static GitService gitService;

  private static Code2Graph c2g;
  private static List<Link> xllLinks = new ArrayList<>();

  public static void init(String framework, String repoPath, String configPath) {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    try {
      gitService = new GitServiceCGit(repoPath);
      c2g = new Code2Graph(repoPath, repoPath, configPath);

      switch (framework) {
        case "springmvc":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.HTML);
          c2g.addSupportedLanguage(Language.JSP);
          break;
        case "android":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.XML);
          break;
        case "mybatis":
          c2g.addSupportedLanguage(Language.JAVA);
          c2g.addSupportedLanguage(Language.XML);
          c2g.addSupportedLanguage(Language.SQL);
          break;
        default:
          c2g.addSupportedLanguage(Language.JAVA);
      }

      logger.info("Detecting broken XLLs for repo {}", repoPath);
    } catch (NonexistPathException | InvalidRepoException | IOException e) {
      e.printStackTrace();
    }
  }

  public static void detectForAllVersion(
      String framework, String repoPath, String configPath, String otPath)
      throws IOException, ParserConfigurationException, SAXException {
    init(framework, repoPath, configPath);

    List<String> commits = gitService.getCommitHistory();
    for (int i = 0; i < commits.size(); i++) {
      detectFor(framework, repoPath, commits.get(i), otPath);
    }
  }

  public static List<BrokenXLL> detectCurrent(String framework, String repoPath, String configPath)
      throws ParserConfigurationException, IOException, SAXException {
    init(framework, repoPath, configPath);

    Pair<Map<String, Set<URI>>, Map<String, Set<URI>>> pair = getUseDefInRule(framework, repoPath);
    Map<String, Set<URI>> useInRule = pair.getLeft();
    Map<String, Set<URI>> defInRule = pair.getRight();

    Set<URI> useSet = new HashSet<>();
    Map<URI, String> useUriToRule = new HashMap<>();
    for (String ruleName : useInRule.keySet()) {
      Set<URI> uses = useInRule.get(ruleName);
      for (URI use : uses) {
        useSet.add(use);
        if (!useUriToRule.containsKey(use)) useUriToRule.put(use, ruleName);
        else {
          String curRule = useUriToRule.get(use);
          useUriToRule.put(use, curRule + "/" + ruleName);
        }
      }
    }

    Set<URI> defSet = new HashSet<>();
    Map<URI, String> defUriToRule = new HashMap<>();
    for (String ruleName : defInRule.keySet()) {
      Set<URI> defs = defInRule.get(ruleName);
      for (URI def : defs) {
        defSet.add(def);
        if (!defUriToRule.containsKey(def)) defUriToRule.put(def, ruleName);
        else {
          String curRule = defUriToRule.get(def);
          defUriToRule.put(def, curRule + "/" + ruleName);
        }
      }
    }

    xllLinks = c2g.getXllLinks();
    Set<URI> useInXLL = xllLinks.stream().map(item -> item.use).collect(Collectors.toSet());
    Set<URI> defInXLL = xllLinks.stream().map(item -> item.def).collect(Collectors.toSet());
    Set<URI> useIntersection = new HashSet<>(useSet);
    Set<URI> defIntersection = new HashSet<>(defSet);
    useIntersection.retainAll(useInXLL);
    useSet.removeAll(useIntersection);
    defIntersection.retainAll(defInXLL);
    defSet.removeAll(defIntersection);

    URITree uriTree = GraphUtil.getUriTree();
    List<BrokenXLL> brokenXLLs = new ArrayList<>();
    for (URI useURI : useSet) {
      List<Node> nodes = uriTree.get(useURI);
      for (Node node : nodes) {
        Range range = node.getRange();
        range.setFileName(useURI.getLayer(0).get("identifier"));
        String language = useURI.getLayer(useURI.getLayerCount() - 1).get("language");
        BrokenXLL broken =
            new BrokenXLL(
                range,
                useUriToRule.get(useURI),
                useURI,
                language.equals("ANY")
                    ? useURI.getLayer(useURI.getLayerCount() - 2).get("language")
                    : language);
        brokenXLLs.add(broken);
      }
    }

    for (URI defURI : defSet) {
      List<Node> nodes = uriTree.get(defURI);
      for (Node node : nodes) {
        Range range = node.getRange();
        if (range == null) {
          range = new Range(0, 0);
        }
        range.setFileName(defURI.getLayer(0).get("identifier"));
        String language = defURI.getLayer(defURI.getLayerCount() - 1).get("language");
        BrokenXLL broken =
            new BrokenXLL(
                range,
                defUriToRule.get(defURI),
                defURI,
                language.equals("ANY")
                    ? defURI.getLayer(defURI.getLayerCount() - 2).get("language")
                    : language);
        brokenXLLs.add(broken);
      }
    }

    return brokenXLLs;
  }

  private static Pair<Map<String, Set<URI>>, Map<String, Set<URI>>> getUseDefInRule(
      String framework, String repoPath)
      throws ParserConfigurationException, SAXException, IOException {
    c2g.getXllLinks().clear();
    GraphUtil.clearGraph();

    if (framework.equals("mybatis")) {
      MybatisPreprocesser.preprocessMapperXmlFile(repoPath);
    }

    Graph<Node, Edge> graph = c2g.generateURIs();
    Pair<Map<String, Set<URI>>, Map<String, Set<URI>>> pair =
        c2g.generateXLLReturnUseSet(GraphUtil.getGraph());

    return pair;
  }

  public static void detectFor(String framework, String repoPath, String commitID, String otPath)
      throws IOException, ParserConfigurationException, SAXException {
    gitService.checkoutByLongCommitID(commitID);

    Pair<Map<String, Set<URI>>, Map<String, Set<URI>>> pair = getUseDefInRule(framework, repoPath);
    Map<String, Set<URI>> useInRule = pair.getLeft();
    Map<String, Set<URI>> defInRule = pair.getRight();

    Set<String> useSet = new HashSet<>();
    Map<String, String> useUriToRule = new HashMap<>();
    for (String ruleName : useInRule.keySet()) {
      Set<URI> uses = useInRule.get(ruleName);
      for (URI use : uses) {
        String useURIStr = use.toString();
        useSet.add(useURIStr);
        if (!useUriToRule.containsKey(useURIStr)) useUriToRule.put(useURIStr, ruleName);
        else {
          String curRule = useUriToRule.get(useURIStr);
          useUriToRule.put(useURIStr, curRule + "/" + ruleName);
        }
      }
    }

    Set<String> defSet = new HashSet<>();
    Map<String, String> defUriToRule = new HashMap<>();
    for (String ruleName : defInRule.keySet()) {
      Set<URI> defs = defInRule.get(ruleName);
      for (URI def : defs) {
        String defURIStr = def.toString();
        defSet.add(defURIStr);
        if (!defUriToRule.containsKey(defURIStr)) defUriToRule.put(defURIStr, ruleName);
        else {
          String curRule = defUriToRule.get(defURIStr);
          defUriToRule.put(defURIStr, curRule + "/" + ruleName);
        }
      }
    }

    xllLinks = c2g.getXllLinks();
    Set<String> useInXLL =
        xllLinks.stream().map(item -> item.use.toString()).collect(Collectors.toSet());
    Set<String> useIntersection = new HashSet<>(useSet);
    useIntersection.retainAll(useInXLL);
    useSet.removeAll(useIntersection);

    Set<String> defInXLL =
        xllLinks.stream().map(item -> item.def.toString()).collect(Collectors.toSet());
    Set<String> defIntersection = new HashSet<>(defSet);
    defIntersection.retainAll(defInXLL);
    defSet.removeAll(defIntersection);

    logger.info("use missing def: {}", useSet);
    logger.info("def missing use: {}", defSet);
    exportResult(useSet, useUriToRule, Path.of(otPath, commitID + "_missing_def.csv").toString());
    exportResult(defSet, defUriToRule, Path.of(otPath, commitID + "_missing_use.csv").toString());
    logger.info("{} XLLs detected for commit {}", useSet.size(), commitID);
    //    exportXLL(xllPath);
  }

  private static void exportXLL(String filePath) throws IOException {
    File file = new File(filePath);
    if (!file.exists()) file.createNewFile();
    CsvWriter writer = new CsvWriter(filePath, ',', StandardCharsets.UTF_8);
    String[] header = {"rule", "def", "use"};
    writer.writeRecord(header);
    for (Link link : xllLinks) {
      writer.writeRecord(new String[] {link.rule.name, link.def.toString(), link.use.toString()});
    }
    writer.close();
  }

  private static void exportResult(Set<String> uris, Map<String, String> uriToRule, String filePath)
      throws IOException {
    if (uris.isEmpty()) return;
    File file = new File(filePath);
    if (!file.exists()) FileUtil.createFile(filePath);
    CsvWriter writer = new CsvWriter(filePath, ',', StandardCharsets.UTF_8);
    String[] header = {"rule", "uri"};
    writer.writeRecord(header);
    for (String uri : uris) {
      writer.writeRecord(new String[] {uriToRule.get(uri), uri});
    }
    writer.close();
  }
}
