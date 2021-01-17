package edu.pku.code2graph.client;

import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    Code2Graph client = new Code2Graph("Code2Graph", System.getProperty("user.dir"));
    // specify
    //    Generator generator = new JdtGenerator();

    try {
      // from files
      List<String> filePaths = new ArrayList<>();
      filePaths.add(
          client.getRepoPath()
              + File.separator
              + "client/src/main/java/edu/pku/code2graph/client/Code2Graph.java");
      Graph<Node, Edge> graph = client.getGenerator().generateFromFiles(filePaths);
      GraphVizExporter.printAsDot(graph);

      // TODO: create a root project node if necessary
      client.getDiffer().computeDiff();

    } catch (IOException e) {
      e.printStackTrace();
    }
    //

  }
}
