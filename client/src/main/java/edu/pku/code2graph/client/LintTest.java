package edu.pku.code2graph.client;

import com.csvreader.CsvReader;
import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Link;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Test on the simulated code smell/side effects of code changes */
public class LintTest {
  // common utils used across methods
  private static Logger logger = LoggerFactory.getLogger(LintTest.class);
  private static Code2Graph c2g;

  // common constants used across methods
  private static String framework = "android";
  private static String repoName = "CloudReader";
  private static String configPath =
      System.getProperty("user.dir") + "/client/src/main/resources/" + framework + "/config.yml";
  private static String repoPath = System.getProperty("user.home") + "/Downloads/lint/" + repoName;

  public static void main(String[] args)
      throws IOException, NonexistPathException, InvalidRepoException {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    String version1 = repoPath + "1";
    String version2 = repoPath + "2";
    System.out.println("V1 at: " + version1);
    System.out.println("V2 at: " + version2);

    try {
      setUp(version1);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }

    List<Link> res = lint(version1, version2);
    res.forEach(System.out::println);
    //  compare with the ground truth list, compute the precision for current case
    String gtPath = repoPath + "_gt.csv";
    CsvReader gtReader = new CsvReader(gtPath);
    gtReader.readHeaders();
    String[] gtHeaders = gtReader.getHeaders();

    Set<String> gtLines = new HashSet<>();
    String leftLang = gtHeaders[0], rightLang = gtHeaders[1];
    while (gtReader.readRecord()) {
      gtLines.add(gtReader.get(leftLang) + "," + gtReader.get(rightLang));
    }
    gtReader.close();

    // compute precision for the current test case

  }

  private static void setUp(String folder)
      throws NonexistPathException, ParserConfigurationException, SAXException {
    c2g = new Code2Graph(repoName, folder, configPath);
    switch (framework) {
      case "springmvc":
        c2g.addSupportedLanguage(Language.JAVA);
        c2g.addSupportedLanguage(Language.HTML);
        break;
      case "android":
        c2g.addSupportedLanguage(Language.JAVA);
        c2g.addSupportedLanguage(Language.XML);
        break;
      case "mybatis":
        c2g.addSupportedLanguage(Language.JAVA);
        c2g.addSupportedLanguage(Language.XML);
        c2g.addSupportedLanguage(Language.SQL);
        MybatisPreprocesser.preprocessMapperXmlFile(repoPath);
        break;
      default:
        c2g.addSupportedLanguage(Language.JAVA);
    }
  }

  private static List<Link> lint(String version1, String version2) {
    // collect all URIs as a tree in v1 and v2
    Graph<Node, Edge> graph1 = c2g.generateGraph(version1);
    List<Link> xllLinks = c2g.getXllLinks();
    URITree tree1 = (URITree) SerializationUtils.clone(GraphUtil.getUriTree());
    GraphUtil.clearGraph();

    Graph<Node, Edge> graph2 = c2g.generateGraph(version2);
    URITree tree2 = GraphUtil.getUriTree();

    System.out.println(tree1.children.size() == tree2.children.size());
    System.out.println(tree1.nodes.size() == tree2.nodes.size());
    // analyze volation in the current change
    List<Link> res = analyzeViolation(xllLinks, tree1, tree2);
    return res;
  }

  private static List<Link> analyzeViolation(List<Link> xllLinks, URITree tree1, URITree tree2) {
    // analyze XLL in tree1
    // compare tree1 with tree2
    // return broken XLL
    return new ArrayList<>();
  }
}
