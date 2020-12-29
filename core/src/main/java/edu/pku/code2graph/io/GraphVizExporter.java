package edu.pku.code2graph.io;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;
import org.jgrapht.Graph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convert graph to dot format of GraphViz
 */
public class GraphVizExporter {

  public static String exportAsDot(Graph<Node, Edge> graph) {
    DOTExporter<Node, Edge> exporter = new DOTExporter<>();

    exporter.setVertexIdProvider(v -> v.getId().toString());
    exporter.setVertexAttributeProvider(
        (v) -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          map.put("type", DefaultAttribute.createAttribute(v.getType().toString()));
          //          map.put("label", DefaultAttribute.createAttribute(v.getSnippet().toString()));
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
}
