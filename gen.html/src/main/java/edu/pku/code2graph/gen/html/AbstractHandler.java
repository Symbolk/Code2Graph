package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

public class AbstractHandler {
  protected Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

  @Deprecated
  protected Graph<Node, Edge> graph = GraphUtil.getGraph();

  // temporarily save the current file path here
  protected String filePath;
  protected String uriFilePath;

  @Deprecated
  protected Stack<ElementNode> stack = new Stack<>();

  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
    this.uriFilePath = FileUtil.getRelativePath(filePath);
  }

  public String getIdentifier(String self) {
    StringBuilder idtf = new StringBuilder();
    for (ElementNode node : stack) {
      if(!node.getName().isEmpty())
        idtf.append(URI.checkInvalidCh(node.getName())).append("/");
    }
    idtf.append(URI.checkInvalidCh(self));
    return idtf.toString();
  }
}
