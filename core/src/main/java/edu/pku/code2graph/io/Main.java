package edu.pku.code2graph.io;

import edu.pku.code2graph.Code2Graph;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;

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
          generator.generateFrom("core/src/main/java/edu/pku/code2graph/Code2Graph.java");
      System.out.println("Got");
    } catch (IOException e) {
      e.printStackTrace();
    }
    //

  }
}
