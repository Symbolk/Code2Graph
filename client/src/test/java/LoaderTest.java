import edu.pku.code2graph.client.Code2Graph;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

public class LoaderTest {
  private void projectTest(String repoName, String configName) {
    String repoPath = FileUtil.getPathFromURL(this.getClass().getResource(repoName));
    String configPath = FileUtil.getPathFromURL(this.getClass().getResource(configName));
    Code2Graph c2g = new Code2Graph(repoName, repoPath, configPath);
    c2g.generateGraph();
  }

  @Test
  public void butterknifeTest() {
    projectTest("android/butterknife/main", "android/config.yml");
  }

  @Test
  public void databindingTest() {
    projectTest("android/databinding/main", "android/config.yml");
  }

  @Test
  public void findviewbyidTest() {
    projectTest("android/findviewbyid/main", "android/config.yml");
  }

  @Test
  public void viewbindingTest() {
    projectTest("android/viewbinding/main", "android/config.yml");
  }
}
