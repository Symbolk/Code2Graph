import edu.pku.code2graph.client.Code2Graph;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Config;
import edu.pku.code2graph.xll.ConfigLoader;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Optional;

public class LoaderTest {
  @Test
  public void main() {
    String repoPath = Objects.requireNonNull(this.getClass().getResource("android/butterknife/main")).toString().substring(6);
    Code2Graph c2g = new Code2Graph("butterknife", repoPath);
    Graph<Node, Edge> graph = c2g.generateGraph();
    GraphVizExporter.printAsDot(graph);
  }
}
