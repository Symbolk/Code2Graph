import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Linker;
import edu.pku.code2graph.xll.URIPattern;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;

import static edu.pku.code2graph.client.CacheHandler.*;

public class CacheTest {
  private static String framework = "springmvc";
  private static String repoName = "sagan";
  private static String configPath =
      System.getProperty("user.dir") + "/src/main/resources/" + framework + "/config.yml";
  private static String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
  private static String cachePath =
      System.getProperty("user.home") + "/coding/xll/cache/" + framework + "/" + repoName;

  @Test
  public void testInitCache() throws IOException, ParserConfigurationException, SAXException {
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
    System.out.println(linker.captures);
  }

  @Test
  public void testUpdateCache() throws IOException, ParserConfigurationException, SAXException {
    String modifiedFile = repoPath;
    updateCache(framework, repoPath, modifiedFile, cachePath);
  }

  @Test
  public void testLoadCache() throws IOException {
    URITree tree = new URITree();
    loadCache(cachePath, tree);

    URIPattern def = new URIPattern(false, "*.java");
    def.addLayer("(&functionName)/(&modelName).addAttribute", Language.JAVA);
    def.addLayer("(name)");

    URIPattern use = new URIPattern(true, "*.html");
    use.addLayer("**", Language.HTML);
    use.addLayer("${(name)}");

    Linker linker = new Linker(tree, def, use);
    linker.link();
    System.out.println(linker.links);
    System.out.println(linker.captures);
  }
}
