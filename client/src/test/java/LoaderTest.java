import edu.pku.code2graph.client.Code2Graph;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

public class LoaderTest {
  private Graph<Node, Edge> load(String repoName, String configName) {
    String repoPath = FileUtil.getPathFromURL(this.getClass().getResource(repoName));
    String configPath = FileUtil.getPathFromURL(this.getClass().getResource(configName));
    Code2Graph c2g = new Code2Graph(repoName, repoPath, configPath);
    return c2g.generateGraph();
  }

  @Test
  public void butterknifeTest() {
    load("android/butterknife/main", "android/config.yml");
  }

  @Test
  public void databindingTest() {
    load("android/databinding/main", "android/config.yml");
  }

  @Test
  public void findviewbyidTest() {
    load("android/findviewbyid/main", "android/config.yml");
  }

  @Test
  public void viewbindingTest() {
    load("android/viewbinding/main", "android/config.yml");
  }

  @Test
  public void mybatisJavaTest() {
    load("mybatis/embedded_in_java/main", "mybatis/config.yml");
  }

  @Test
  public void mybatisXMLTest() {
    load("mybatis/embedded_in_xml/main", "mybatis/config.yml");
  }

  @Test
  public void saganTest() {
    load("spring/sagan", "spring/config.yml");
  }
}
