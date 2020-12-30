import edu.pku.code2graph.io.Neo4jExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static edu.pku.code2graph.model.TypeSet.type;
import static org.assertj.core.api.Assertions.assertThat;

public class Neo4jExporterTest {
  @Test
  public void testCreateNodes() {
    Graph<Node, Edge> graph = GraphUtil.initGraph();
    Type type = type("ASSIGNMENT_OPERATOR");
    graph.addVertex(new ElementNode(0, type, "=", "equals", "equals"));
    graph.addVertex(new ElementNode(1, type, ">", "gt", "gt"));
    graph.addVertex(new ElementNode(2, type, "<", "lt", "lt"));
    Neo4jExporter.export(graph);
    Integer id = Neo4jExporter.queryByID(0);
    assertThat(id).isEqualTo(0);
    List<String> names = Neo4jExporter.queryByType(type);
    assertThat(names.size()).isEqualTo(3);
    // remove inserted nodes
  }
}
