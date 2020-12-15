package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;

import java.io.IOException;

@Register(id = "java-jdt", accept = "\\.java$", priority = Registry.Priority.MAXIMUM)
public class JdtGenerator extends Generator {
  @Override
  protected Graph<Node, Edge> generate() throws IOException {
    return null;
  }
}
