package edu.pku.code2graph.io;

import edu.pku.code2graph.Code2Graph;
import edu.pku.code2graph.Generator;
import edu.pku.code2graph.model.Language;

public class Main {
  public static void main(String[] args) {
    // create
    Code2Graph client = new Code2Graph("Code2Graph", System.getProperty("user.dir"));
    // analyze a single file
    Generator generator = client.getGenerator(Language.JAVA);
    generator.generateFrom().file("core/src/main/java/edu/pku/code2graph/Code2Graph.java");
    //

  }
}
