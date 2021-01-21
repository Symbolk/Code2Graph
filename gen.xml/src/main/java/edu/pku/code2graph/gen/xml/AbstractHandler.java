package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
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
}
