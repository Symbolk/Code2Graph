import edu.pku.code2graph.exception.InvalidRepoException;
import edu.pku.code2graph.exception.NonexistPathException;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Linker;
import edu.pku.code2graph.xll.URIPattern;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static edu.pku.code2graph.cache.CacheHandler.*;


public class CacheTest {
  private static String framework = "android";
  private static String repoName = "CloudReader";
  private static String configPath =
      System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";
  private static String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
  private static String cachePath =
      System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;

  private void setUp(String frame, String repo) {
    framework = frame;
    repoName = repo;

    configPath =
        System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";
    repoPath = System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
    cachePath = System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;
  }

  @Test
  public void testInitCache() throws IOException, ParserConfigurationException, SAXException {
    setUp("springmvc", "sagan");
    initCache(framework, repoPath, cachePath);

    URIPattern def = new URIPattern(false, "*.java");
    def.addLayer("(&functionName)/(&modelName).addAttribute", Language.JAVA);
    def.addLayer("(name)");

    URIPattern use = new URIPattern(true, "*.html");
    use.addLayer("**", Language.HTML);
    use.addLayer("${(name)}");

    Linker linker = new Linker(GraphUtil.getUriTree(), def, use);
    linker.link();
    System.out.println(linker.links);
    System.out.println(linker.context);
  }

  @Test
  public void testUpdateCache() throws IOException, ParserConfigurationException, SAXException {
    setUp("springmvc", "sagan");
    String modifiedFile = repoPath + "/sagan-site/src/main/resources/templates/admin/show.html";
    updateCache(framework, repoPath, modifiedFile, cachePath);
    URITree tree = GraphUtil.getUriTree();
    System.out.println(tree);
  }

  @Test
  public void testLoadCache() throws IOException {
    setUp("springmvc", "sagan");

    URITree tree = new URITree();
    loadCache(cachePath, tree, null, null);

    URIPattern def = new URIPattern(false, "*.java");
    def.addLayer("(&functionName)/(&modelName).addAttribute", Language.JAVA);
    def.addLayer("(name)");

    URIPattern use = new URIPattern(true, "*.html");
    use.addLayer("**", Language.HTML);
    use.addLayer("${(name)}");

    Linker linker = new Linker(tree, def, use);
    linker.link();
    System.out.println(linker.links);
    System.out.println(linker.context);
  }

  @Test
  public void testSHA()
      throws IOException, ParserConfigurationException, NoSuchAlgorithmException, SAXException {
    setUp("android", "CloudReader");
    cachePath =
        System.getProperty("user.home") + "/coding/xll/sha-cache/" + framework + "/" + repoName;
    initCache(framework, repoPath, cachePath, true);
  }

  @Test
  public void testSHALoad()
      throws ParserConfigurationException, NoSuchAlgorithmException, IOException, SAXException,
          NonexistPathException, InvalidRepoException {
    setUp("android", "CloudReader");
    repoPath =
        System.getProperty("user.dir") + "/src/main/resources/" + framework + "/repos/" + repoName;
    cachePath =
        System.getProperty("user.home") + "/coding/xll/sha-history/" + framework + "/" + repoName;
    URITree tree = GraphUtil.getUriTree();
    loadFor(framework, repoPath, cachePath, "2a0ac6cb6a9191992458d6b7ff9bba48244be085", tree);
  }

  @Test
  public void testAndroid() throws IOException, ParserConfigurationException, SAXException {
    setUp("android", "CloudReader");
    initCache(framework, repoPath, cachePath);
  }
}
