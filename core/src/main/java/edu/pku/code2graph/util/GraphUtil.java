package edu.pku.code2graph.util;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

public class GraphUtil {

  /**
   * Initialize an empty Graph, return the instance
   *
   * @return
   */
  public static Graph<Node, Edge> initGraph() {
    return GraphTypeBuilder.<Node, Edge>directed()
        .allowingMultipleEdges(true) // allow multiple edges with different types
        .allowingSelfLoops(true) // allow recursion
        .edgeClass(Edge.class)
        .weighted(true)
        .buildGraph();
  }

  /**
   * Generate a unique and incremental id for node TODO support parallel security
   *
   * @return
   */
  public static Integer popNodeID(Graph graph) {
    return graph.vertexSet().size() + 1;
  }

  /**
   * Generate a unique and incremental id for edge TODO support parallel security
   *
   * @return
   */
  public static Integer popEdgeID(Graph graph) {
    return graph.edgeSet().size() + 1;
  }
}
