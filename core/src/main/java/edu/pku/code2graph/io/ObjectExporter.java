package edu.pku.code2graph.io;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;
import org.jgrapht.Graph;

public class ObjectExporter {
  public static void exportObjectToFile(Graph<Node, Edge> graph, String path) {
    FileUtil.writeObjectToFile(graph, path, false);
  }
}
