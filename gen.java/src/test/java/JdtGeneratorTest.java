import edu.pku.code2graph.gen.jdt.JdtGenerator;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JdtGeneratorTest {
  private static final JdtGenerator generator = new JdtGenerator();

  JdtGeneratorTest() {
    FileUtil.setRootPath(new File("build/resources/test").getAbsolutePath());
  }

  private void generateGraph(String name) {
    try {
      List<String> filePaths = new ArrayList<>();
      filePaths.add(this.getClass().getResource(name).getPath());
      Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
      printNodes(graph);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static void printNodes(Graph<Node, Edge> graph) {
    StringBuilder builder = new StringBuilder();
    for (Node node : graph.vertexSet()) {
      URI uri = node.getUri();
      builder.append(node.getRange()).append(" ");
      if (uri != null) {
        builder.append(uri);
      } else {
        builder.append(node instanceof ElementNode
            ? node.getType().name + "(" + ((ElementNode) node).getName() + ")"
            : node.getType().name + "(" + ((RelationNode) node).getSymbol() + ")");
      }
      builder.append("\n");
    }
    System.out.println(builder);
  }

  @Test
  public void testMember() {
    generateGraph("TestMember.java");
  }

  @Test
  public void testAnnotation() {
    generateGraph("TestAnnotation.java");
  }

  @Test
  public void testEnum() {
    generateGraph("TestEnum.java");
  }

  @Test
  public void testController() {
    generateGraph("BlogController.java");
  }
}
