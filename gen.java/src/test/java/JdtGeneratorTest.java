import edu.pku.code2graph.gen.jdt.JdtGenerator;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.FileUtil;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
      GraphVizExporter.printNodes(graph);
    } catch (Exception e) {
      e.printStackTrace();
    }
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
}
