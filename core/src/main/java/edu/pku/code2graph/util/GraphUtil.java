package edu.pku.code2graph.util;

import edu.pku.code2graph.model.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphUtil {
  // singleton across a graph building process (but for diff?)
  private static Graph<Node, Edge> graph;
  // FIXME use global counter as a compromise for performance,
  // BUT may skip or jump an id if adding node or edge failed (which means graph not changed)
  private static Integer nodeCount;
  private static Integer edgeCount;
  // sets of URIs that possibly have XLL
  private static Map<Language, Map<URI, List<Node>>> uriMap;

  static {
    graph = initGraph();
    nodeCount = 0;
    edgeCount = 0;
    uriMap = new HashMap<>();
  }

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
   * Generate a unique and incremental id for node
   *
   * @return
   */
  public static Integer nid() {
    // return graph.vertexSet().size() + 1;
    return ++nodeCount;
  }

  /**
   * Generate a unique and incremental id for edge
   *
   * @return
   */
  public static Integer eid() {
    return ++edgeCount;
  }

  public static Graph<Node, Edge> getGraph() {
    return graph;
  }

  public static void clearGraph() {
    graph = initGraph();
    nodeCount = 0;
    edgeCount = 0;
    uriMap = new HashMap<>();
  }

  /**
   * Add one single uri to the urimap
   *
   * @param language
   * @param uri
   * @param node
   */
  public static void addURI(Language language, URI uri, Node node) {
    uriMap
        .computeIfAbsent(language, k -> new HashMap<>())
        .computeIfAbsent(uri, k -> new ArrayList<>())
        .add(node);
  }

  /**
   * Add a collection of uris into the existing urimap
   *
   * @param language
   * @param map
   */
  public static void addURIs(Language language, Map<URI, List<Node>> map) {
    map.forEach(
        (key, value) -> {
          value.forEach(v -> {
            addURI(language, key, v);
          });
        });
  }

  public static Map<Language, Map<URI, List<Node>>> getUriMap() {
    return uriMap;
  }
}
