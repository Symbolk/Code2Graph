package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
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

  // temporarily save the current file path here
  protected String filePath;
  protected String uriFilePath;

  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
    this.uriFilePath = FileUtil.getRelativePath(filePath);
  }
}
