package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.Differ;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.apache.log4j.BasicConfigurator;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
  private static String tempDir = System.getProperty("user.home") + "/coding/data/temp/c2g";

  public static void main(String[] args) {
    // config the logger with log4j
    //    System.out.println(System.getProperty("user.dir"));
    //        System.setProperty("logs.dir", System.getProperty("user.dir"));
    //    PropertyConfigurator.configure("log4j.properties"); // Note that this could lead to
    // log4j.properties in jar dependencies
    //    // use basic configuration when packaging
    BasicConfigurator.configure();
    //    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
    //    testDiff();
    testFiles();
  }

  private static void testDiff() {
    Code2Graph client =
        new Code2Graph("cxf", System.getProperty("user.home") + "/coding/data/repos/cxf", tempDir);

    // TODO: create a root project node if necessary
    try {
      Differ differ = client.getDiffer();
      // TODO hide concrete method calls for diff as one public API
      differ.buildGraphs("ed4faad");
      //      GraphVizExporter.copyAsDot(differ.getaGraph());
      differ.compareGraphs();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void testFiles() {
    Code2Graph client = new Code2Graph("Code2Graph", System.getProperty("user.dir"), tempDir);

    // specify
    //    Generator generator = new JdtGenerator();

    // from files
    List<String> filePaths = new ArrayList<>();
    filePaths.add(
        client.getRepoPath()
            + File.separator
            + "client/src/main/java/edu/pku/code2graph/client/Code2Graph.java");
    Graph<Node, Edge> graph = client.generateGraph(filePaths);
    GraphVizExporter.printAsDot(graph);
  }
}
