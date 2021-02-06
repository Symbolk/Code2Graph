package edu.pku.code2graph.io;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.RelationNode;
import edu.pku.code2graph.util.FileUtil;
import org.jgrapht.Graph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Convert graph to dot format of GraphViz */
public class GraphVizExporter {

  public static String exportAsDot(Graph<Node, Edge> graph) {
    DOTExporter<Node, Edge> exporter = new DOTExporter<>();

    exporter.setVertexIdProvider(v -> v.getId().toString());
    exporter.setVertexAttributeProvider(
        v -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          // new jgrapht API has no dedicated label provider setter
          map.put("id", DefaultAttribute.createAttribute(v.getId().toString()));
          map.put("type", DefaultAttribute.createAttribute(v.getType().toString()));
          map.put(
              "label",
              DefaultAttribute.createAttribute(
                  v instanceof ElementNode
                      ? v.getType().name + "(" + ((ElementNode) v).getName() + ")"
                      : v.getType().name + "(" + ((RelationNode) v).getSymbol() + ")"));
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

  public static String exportAsDot(
      Graph<Node, Edge> graph, Set<Node> removed, Set<Node> added, Set<Node> unchanged) {
    DOTExporter<Node, Edge> exporter = new DOTExporter<>();

    exporter.setVertexIdProvider(v -> v.getId().toString());
    exporter.setVertexAttributeProvider(
        v -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          // new jgrapht API has no dedicated label provider setter
          map.put("id", DefaultAttribute.createAttribute(v.getId().toString()));
          map.put("type", DefaultAttribute.createAttribute(v.getType().toString()));
          map.put(
              "label",
              DefaultAttribute.createAttribute(
                  v instanceof ElementNode
                      ? v.getType().name + "(" + ((ElementNode) v).getName() + ")"
                      : v.getType().name + "(" + ((RelationNode) v).getSymbol() + ")"));
          map.put("shape", new NodeShapeAttribute(v));
          map.put(
              "color",
              DefaultAttribute.createAttribute(
                  removed.contains(v) ? "red" : (added.contains(v) ? "green" : "grey")));

          return map;
        });

    exporter.setEdgeIdProvider(e -> e.getId().toString());
    exporter.setEdgeAttributeProvider(
        e -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          map.put("type", DefaultAttribute.createAttribute(e.getType().toString()));
          map.put("label", DefaultAttribute.createAttribute(e.getType().name));
          map.put("color", DefaultAttribute.createAttribute("grey"));
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
   * Assign different color for removed, added, and context unchanged nodes
   *
   * @param graph
   * @param removed
   * @param added
   * @param unchanged
   */
  public static void printAsDot(
      Graph<Node, Edge> graph, Set<Node> removed, Set<Node> added, Set<Node> unchanged) {
    System.out.println(exportAsDot(graph, removed, added, unchanged));
  }

  /**
   * Copy the dot into the system clipboard
   *
   * @param graph
   * @return
   */
  public static String copyAsDot(Graph<Node, Edge> graph) {
    String dot = exportAsDot(graph);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    Transferable transferable = new StringSelection(dot);
    clipboard.setContents(transferable, null);
    return dot;
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
