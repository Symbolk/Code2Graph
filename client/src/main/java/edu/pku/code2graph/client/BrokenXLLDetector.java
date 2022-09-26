package edu.pku.code2graph.client;

import com.csvreader.CsvWriter;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.gen.xml.MybatisPreprocesser;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class BrokenXLLDetector {
  private static Logger logger = LoggerFactory.getLogger(BrokenXLLDetector.class);
  private static final String framework = "android";
  private static final String repoName = "Timber";
  //  private static final String commitID = "f82d7d73e9cb5f764b305008fea6cfcc47a21ac4";
  private static String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
  private static String configPath =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/broken/config.yml";
  private static String otPath =
      System.getProperty("user.home") + "/coding/broken/" + framework + "/" + repoName;

  private static GitService gitService;

  private static Code2Graph c2g;
  private static List<Link> xllLinks = new ArrayList<>();

  public static void main(String[] args) {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    try {
      gitService = new GitServiceCGit(repoPath);
      c2g = new Code2Graph(repoName, repoPath, configPath);

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

      logger.info("Detecting broken XLLs for repo {}", repoName);

      detectForAllVersion();
    } catch (NonexistPathException
        | IOException
        | InvalidRepoException
        | ParserConfigurationException
        | SAXException e) {
      e.printStackTrace();
    }
  }

  private static void detectForAllVersion()
      throws IOException, ParserConfigurationException, SAXException {
    List<String> commits = gitService.getCommitHistory();
    for (int i = 0; i < commits.size(); i++) {
      detectFor(commits.get(i));
    }
  }

  private static void detectFor(String commitID)
      throws IOException, ParserConfigurationException, SAXException {
    logger.info("Detecting  for repo {}", repoName);
    gitService.checkoutByLongCommitID(commitID);
    c2g.getXllLinks().clear();
    GraphUtil.clearGraph();

    if (framework.equals("mybatis")) {
      MybatisPreprocesser.preprocessMapperXmlFile(repoPath);
    }

    Graph<Node, Edge> graph = c2g.generateURIs();
    Set<String> useSet = new HashSet<>();
    Map<String, String> uriToRule = new HashMap<>();
    Map<String, Set<URI>> useInRule = c2g.generateXLLReturnUseSet(GraphUtil.getGraph());
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
    exportResult(useSet, uriToRule, otPath + "/" + commitID + ".csv");
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
