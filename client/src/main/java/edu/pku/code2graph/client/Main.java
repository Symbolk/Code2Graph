package edu.pku.code2graph.client;

import edu.pku.code2graph.gen.Generators;
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
    // create
    Code2Graph client = new Code2Graph("Code2Graph", System.getProperty("user.dir"));
    // analyze a single file
    Generators generator = Generators.getInstance();
    //    Generator generator = new JdtGenerator();

    try {
      // from files
      List<String> filePaths = new ArrayList<>();
      filePaths.add(
          client.getRepoPath()
              + File.separator
              + "client/src/main/java/edu/pku/code2graph/client/Code2Graph.java");
      Graph<Node, Edge> graph = generator.generateFromFiles(filePaths);
      GraphVizExporter.printAsDot(graph);

      // TODO: create a root project node if necessary
      // from working tree: 2 graphs: old and new

      // from one commit: 2 graphs: left and right

      // extract changed/diff files and involved ones (optional)

      // collect the content and saves to a temp dir (optional)

      // pass the file paths (on disk)/content (in memory) to the generator

    } catch (IOException e) {
      e.printStackTrace();
    }
    //

  }
}
