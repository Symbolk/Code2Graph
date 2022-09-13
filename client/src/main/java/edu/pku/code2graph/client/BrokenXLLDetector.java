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
  private static final String repoName = "CloudReader";
  private static final String commitID = "91c8334";
  private static String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
  private static String configPath =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/broken/config.yml";
  private static String otPath =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/broken/"
          + repoName
          + ":"
          + commitID
          + ".csv";
  private static String xllPath =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/broken/XLL_"
          + repoName
          + ":"
          + commitID
          + ".out";

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

      logger.info("Generating graph for repo {}:{}", repoName, gitService.getHEADCommitId());

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
      //      Set<String> useSet =
      //          c2g.generateXLLReturnUseSet(GraphUtil.getGraph()).values().stream()
      //              .reduce(
      //                  new HashSet<>(),
      //                  (res, item) -> {
      //                    res.addAll(item);
      //                    return res;
      //                  })
      //              .stream()
      //              .map(item -> item.toString())
      //              .collect(Collectors.toSet());
      xllLinks = c2g.getXllLinks();
      Set<String> useInXLL =
          xllLinks.stream().map(item -> item.use.toString()).collect(Collectors.toSet());

      Set<String> intersection = new HashSet<>(useSet);
      intersection.retainAll(useInXLL);
      int interSize = intersection.size(), setSize = useSet.size(), inXLLSize = useInXLL.size();
      useSet.removeAll(intersection);
      logger.info("use missing def: {}", useSet);
      exportResult(useSet, uriToRule, otPath);
      exportXLL(xllPath);
    } catch (NonexistPathException
        | IOException
        | InvalidRepoException
        | ParserConfigurationException
        | SAXException e) {
      e.printStackTrace();
    }
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
    File file = new File(filePath);
    if (!file.exists()) file.createNewFile();
    CsvWriter writer = new CsvWriter(filePath, ',', StandardCharsets.UTF_8);
    String[] header = {"rule", "uri"};
    writer.writeRecord(header);
    for (String uri : uris) {
      writer.writeRecord(new String[] {uriToRule.get(uri), uri});
    }
    writer.close();
  }
}
