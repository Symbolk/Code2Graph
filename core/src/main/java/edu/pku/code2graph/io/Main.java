package edu.pku.code2graph.io;

import edu.pku.code2graph.Code2Graph;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.jdt.JdtGenerator;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    // create
    Code2Graph client = new Code2Graph("Code2Graph", System.getProperty("user.dir"));
    // analyze a single file
    //    Generators generator = Generators.getInstance();
    Generator generator = new JdtGenerator();

    try {
      Graph<Node, Edge> graph =
          generator.generateFrom().file("core/src/main/java/edu/pku/code2graph/Code2Graph.java");
    } catch (IOException e) {
      e.printStackTrace();
    }
    //

  }
}
