package edu.pku.code2graph.io;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;
import org.jgrapht.Graph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/** Convert graph to dot format of GraphViz */
public class GraphVizExporter {

  public static String exportAsDot(Graph<Node, Edge> graph) {
    DOTExporter<Node, Edge> exporter = new DOTExporter<>();

    exporter.setVertexIdProvider(v -> v.getId().toString());
    exporter.setVertexAttributeProvider(
        v -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          // new jgrapht API has no dedicated label provider setter
          map.put("type", DefaultAttribute.createAttribute(v.getType().toString()));
          map.put(
              "label",
              DefaultAttribute.createAttribute(
                  v instanceof ElementNode
                      ? v.getType().name + "(" + ((ElementNode) v).getName() + ")"
                      : v.getType().name));
          map.put("shape", new NodeShapeAttribute(v));

          return map;
        });

    exporter.setEdgeIdProvider(e -> e.getId().toString());
    exporter.setEdgeAttributeProvider(
        e -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          map.put("type", DefaultAttribute.createAttribute(e.getType().toString()));
          map.put("label", DefaultAttribute.createAttribute(e.getType().name));
          //            map.put("color", new EdgeColorAttribute(edge));
          //            map.put("style", new EdgeStyleAttribute(edge));
          return map;
        });

    Writer writer = new StringWriter();
    exporter.exportGraph(graph, writer);
    return writer.toString();
  }

  /**
   * Print the graph to console for debugging
   *
   * @param graph
   */
  public static void printAsDot(Graph<Node, Edge> graph) {
    System.out.println(exportAsDot(graph));
  }

  /**
   * Save the exported dot to file
   *
   * @param graph
   */
  public static void saveAsDot(Graph<Node, Edge> graph, String filePath) {
    FileUtil.writeStringToFile(exportAsDot(graph), filePath);
  }

  static class NodeShapeAttribute implements Attribute {
    private Node node;

    public NodeShapeAttribute(Node node) {
      this.node = node;
    }

    @Override
    public String getValue() {
      switch (node.getClass().getSimpleName()) {
        case "ElementNode":
          return "folder";
//        case "DataNode":
//          return "note";
//        case "OperationNode":
//          return "cds";
//        case "BlockNode":
//          return "component";
        case "RelationNode":
          return "diamond";
        default:
          return "";
      }
    }

    @Override
    public AttributeType getType() {
      return AttributeType.STRING;
    }
  }
}
