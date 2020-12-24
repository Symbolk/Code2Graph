package edu.pku.code2graph.client;

import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    // create
    Code2Graph client = new Code2Graph("Code2Graph", System.getProperty("user.dir"));
    // analyze a single file
    Generators generator = Generators.getInstance();
    //    Generator generator = new JdtGenerator();

    try {
      Graph<Node, Edge> graph =
          generator.generateFrom(
              client.getRepoPath()
                  + File.separator
                  + "client/src/main/java/edu/pku/code2graph/client/Code2Graph.java");
      System.out.println(graph);
    } catch (IOException e) {
      e.printStackTrace();
    }
    //

  }
}
