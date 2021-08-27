import edu.pku.code2graph.client.Code2Graph;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

import java.util.Objects;

public class LoaderTest {
  private void projectTest(String repoName, String configName) {
    String repoPath = Objects.requireNonNull(this.getClass().getResource(repoName)).getPath();
    String configPath = Objects.requireNonNull(this.getClass().getResource(configName)).getPath();
    Code2Graph c2g = new Code2Graph("butterknife", repoPath, configPath);
    Graph<Node, Edge> graph = c2g.generateGraph();
    GraphVizExporter.printAsDot(graph);
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
