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

  private static void init(String framework, String repoPath, String configPath) {
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

  public static void detectForAllVersion(String framework, String repoPath, String configPath, String otPath)
          throws IOException, ParserConfigurationException, SAXException {
    init(framework, repoPath, configPath);

    List<String> commits = gitService.getCommitHistory();
    for (int i = 0; i < commits.size(); i++) {
      detectFor(framework, repoPath, commits.get(i), otPath);
    }
  }

  public static List<BrokenXLL> detectCurrent(String framework, String repoPath, String configPath) throws ParserConfigurationException, IOException, SAXException {
    init(framework, repoPath, configPath);

    Map<String, Set<URI>> useInRule = getUseInRule(framework, repoPath);

    Set<URI> useSet = new HashSet<>();
    Map<URI, String> uriToRule = new HashMap<>();
    for (String ruleName : useInRule.keySet()) {
      Set<URI> uses = useInRule.get(ruleName);
      for (URI use : uses) {
        useSet.add(use);
        if (!uriToRule.containsKey(use)) uriToRule.put(use, ruleName);
        else {
          String curRule = uriToRule.get(use);
          uriToRule.put(use, curRule + "/" + ruleName);
        }
      }
    }

    xllLinks = c2g.getXllLinks();
    Set<URI> useInXLL =
            xllLinks.stream().map(item -> item.use).collect(Collectors.toSet());
    Set<URI> intersection = new HashSet<>(useSet);
    intersection.retainAll(useInXLL);
    int interSize = intersection.size(), setSize = useSet.size(), inXLLSize = useInXLL.size();
    useSet.removeAll(intersection);

    URITree uriTree = GraphUtil.getUriTree();
    List<BrokenXLL> brokenXLLs = new ArrayList<>();
    for (URI useURI : useSet) {
      List<Node> nodes = uriTree.get(useURI);
      for (Node node : nodes) {
        Range range = node.getRange();
        range.setFileName(useURI.getLayer(0).get("identifier"));
        String language = useURI.getLayer(useURI.getLayerCount() - 1).get("language");
        BrokenXLL broken = new BrokenXLL(range, uriToRule.get(useURI), useURI,
                language.equals("ANY") ? useURI.getLayer(useURI.getLayerCount() - 2).get("language") : language);
        brokenXLLs.add(broken);
      }
    }

    return brokenXLLs;
  }

  private static Map<String, Set<URI>> getUseInRule(String framework, String repoPath) throws ParserConfigurationException, SAXException, IOException {
    c2g.getXllLinks().clear();
    GraphUtil.clearGraph();

    if (framework.equals("mybatis")) {
      MybatisPreprocesser.preprocessMapperXmlFile(repoPath);
    }

    Graph<Node, Edge> graph = c2g.generateURIs();
    Map<String, Set<URI>> useInRule = c2g.generateXLLReturnUseSet(GraphUtil.getGraph());

    return useInRule;
  }

  private static void detectFor(String framework, String repoPath, String commitID, String otPath)
          throws IOException, ParserConfigurationException, SAXException {
    gitService.checkoutByLongCommitID(commitID);

    Map<String, Set<URI>> useInRule = getUseInRule(framework, repoPath);
    Set<String> useSet = new HashSet<>();
    Map<String, String> uriToRule = new HashMap<>();
    for (String ruleName : useInRule.keySet()) {
      Set<URI> uses = useInRule.get(ruleName);
      for (URI use : uses) {
        String useURIStr = use.toString();
        useSet.add(useURIStr);
        if (!uriToRule.containsKey(useURIStr)) uriToRule.put(useURIStr, ruleName);
        else {
          String curRule = uriToRule.get(useURIStr);
          uriToRule.put(useURIStr, curRule + "/" + ruleName);
        }
      }
    }

    xllLinks = c2g.getXllLinks();
    Set<String> useInXLL =
            xllLinks.stream().map(item -> item.use.toString()).collect(Collectors.toSet());
    Set<String> intersection = new HashSet<>(useSet);
    intersection.retainAll(useInXLL);
    int interSize = intersection.size(), setSize = useSet.size(), inXLLSize = useInXLL.size();
    useSet.removeAll(intersection);
    logger.info("use missing def: {}", useSet);
    exportResult(useSet, uriToRule, Path.of(otPath, commitID + ".csv").toString());
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
      writer.writeRecord(new String[]{link.rule.name, link.def.toString(), link.use.toString()});
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
      writer.writeRecord(new String[]{uriToRule.get(uri), uri});
    }
    writer.close();
  }
}
