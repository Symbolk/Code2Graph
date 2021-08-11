package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

public class AbstractHandler {
  protected Logger logger = LoggerFactory.getLogger(DocumentHandler.class);

  protected Graph<Node, Edge> graph = GraphUtil.getGraph();

  // temporarily save the current file path here
  protected String filePath;

  protected Stack<ElementNode> stk = new Stack<>();

  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getIdentifier(String self) {
    String idtf = "";
    for (ElementNode node : stk) {
      idtf = idtf + URI.checkInvalidCh(node.getName()) + "/";
    }
    idtf = idtf + self;
    return idtf;
  }
}
