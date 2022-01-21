package edu.pku.code2graph.io;

import edu.pku.code2graph.model.*;
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
  public static void printNodes(Graph<Node, Edge> graph) {
    StringBuilder builder = new StringBuilder();
    for (Node node : graph.vertexSet()) {
      URI uri = node.getUri();
      builder.append(node.getRange()).append(" ");
      if (uri != null) {
        builder.append(uri.toString());
      } else {
        builder.append(node instanceof ElementNode
                ? node.getType().name + "(" + ((ElementNode) node).getName() + ")"
                : node.getType().name + "(" + ((RelationNode) node).getSymbol() + ")");
      }
      builder.append("\n");
    }
    System.out.println(builder.toString());
  }

  public static String exportAsDot(Graph<Node, Edge> graph, boolean print) {
    DOTExporter<Node, Edge> exporter = new DOTExporter<>();

    exporter.setVertexIdProvider(v -> v.getId().toString());
    exporter.setVertexAttributeProvider(
        v -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          map.put("id", DefaultAttribute.createAttribute(v.getId().toString()));

          URI uri = v.getUri();
          if (uri != null) {
            String output = uri.toString();
            map.put(
                "uri", DefaultAttribute.createAttribute(output.substring(1, output.length() - 1)));
          }

          if (!print || uri == null) {
            // new jgrapht API has no dedicated label provider setter
            map.put("type", DefaultAttribute.createAttribute(v.getType().toString()));
            map.put(
                "label",
                DefaultAttribute.createAttribute(
                    v instanceof ElementNode
                        ? v.getType().name + "(" + ((ElementNode) v).getName() + ")"
                        : v.getType().name + "(" + ((RelationNode) v).getSymbol() + ")"));
            map.put("shape", new NodeShapeAttribute(v));
            map.put(
                "style",
                DefaultAttribute.createAttribute(v instanceof ElementNode ? "solid" : "dashed"));
            //            map.put("fontcolor", DefaultAttribute.createAttribute("white"));
            map.put(
                "color",
                DefaultAttribute.createAttribute(
                    v instanceof ElementNode ? "red" : "blue"));
            //            map.put(
            //                "color",
            //                DefaultAttribute.createAttribute(v instanceof ElementNode ? "red" :
            // "green"));
          }

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

  public static String exportAsDot(Graph<Node, Edge> graph) {
    return exportAsDot(graph, false);
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

          if (v instanceof ElementNode) {
            URI uri = ((ElementNode) v).getUri();
            if (uri != null) {
              String output = uri.toString();
              map.put(
                  "uri",
                  DefaultAttribute.createAttribute(output.substring(1, output.length() - 1)));
            }
          }

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
    System.out.println(exportAsDot(graph, true));
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
    System.out.println("Graph in dot format has been copied!");
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
          return "ellipse";
          //        case "DataNode":
          //          return "note";
          //        case "OperationNode":
          //          return "cds";
          //        case "BlockNode":
          //          return "component";
        case "RelationNode":
          return "hexagon";
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
