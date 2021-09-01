import edu.pku.code2graph.gen.jdt.JdtGenerator;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JdtGeneratorTest {
  private static final JdtGenerator generator = new JdtGenerator();

  private Set<Node> filterNodesByType(Graph<Node, Edge> graph, Type nodeType) {
    Set<Node> result =
        graph.vertexSet().stream()
            .filter(node -> node.getType().equals(nodeType))
            .collect(Collectors.toSet());
    return result;
  }

  @Test
  public void testMethodInvocation() throws IOException {
    List<String> filePaths = new ArrayList<>();
    filePaths.add("src/test/resources/TestMethod.java");
    Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
    assertThat(filterNodesByType(graph, NodeType.METHOD_INVOCATION).size()).isEqualTo(4);
  }

  @Test
  public void testAnnotation() throws IOException {
    List<String> filePaths = new ArrayList<>();
    filePaths.add("src/test/resources/TestAnnotation.java");
    Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
    GraphVizExporter.printAsDot(graph);
  }

  @Test
  public void testEnum() throws IOException {
    List<String> filePaths = new ArrayList<>();
    filePaths.add("src/test/resources/TestEnum.java");
    Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
    GraphVizExporter.printAsDot(graph);
  }
}
