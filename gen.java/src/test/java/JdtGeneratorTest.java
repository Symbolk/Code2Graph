import edu.pku.code2graph.gen.jdt.JdtGenerator;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JdtGeneratorTest {
  private static final JdtGenerator generator = new JdtGenerator();

  @Test
  public void testMethodInvocation() throws IOException {
    try {
      List<String> filePaths = new ArrayList<>();
      filePaths.add("src/test/resources/TestSwitch.java");
      Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
      GraphVizExporter.printAsDot(graph);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
