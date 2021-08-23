package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractHandler extends DefaultHandler {
  protected Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

  protected Graph<Node, Edge> graph = GraphUtil.getGraph();
  protected Map<URI, ElementNode> uriMap = new HashMap<>();

  // temporarily save the current file path here
  protected String filePath;

  // unified identifier: @type/id
  protected Map<String, Node> defPool = new HashMap<>();
  protected List<Triple<Node, Type, String>> usePool = new ArrayList<>();

  /** Build edges with cached data pool */
  public void buildEdges() {
    for (Triple<Node, Type, String> entry : usePool) {
      Node src = entry.getFirst();
      Node tgt = defPool.get(entry.getThird());
      if (tgt != null) {
        graph.addEdge(src, tgt, new Edge(GraphUtil.eid(), entry.getSecond()));
      }
    }
  }

  public Graph<Node, Edge> getGraph() {
    return graph;
  }
  public Map<URI, ElementNode> getUriMap() {
    return uriMap;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }
}
