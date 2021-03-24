import edu.pku.code2graph.gen.jdt.JdtGenerator;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JdtGeneratorTest {
  private static final JdtGenerator generator = new JdtGenerator();

  @Test
  public void testMethodInvocation() throws IOException {
    try {
      List<String> filePaths = new ArrayList<>();
      filePaths.add("src/test/resources/TestFor.java");
      Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
      String dot = GraphVizExporter.copyAsDot(graph);
      assertThat(dot).startsWith("digraph");

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
