package edu.pku.code2graph.client;

import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.apache.log4j.PropertyConfigurator;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    // config the logger with log4j
//    System.setProperty("logs.dir", System.getProperty("user.dir"));
    PropertyConfigurator.configure("log4j.properties");
    //    // use basic configuration when packaging
    //    BasicConfigurator.configure();
    //    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

    Code2Graph client = new Code2Graph("Code2Graph", System.getProperty("user.dir"));
    // specify
    //    Generator generator = new JdtGenerator();

    // from files
    List<String> filePaths = new ArrayList<>();
    filePaths.add(
        client.getRepoPath()
            + File.separator
            + "client/src/main/java/edu/pku/code2graph/client/Code2Graph.java");
    try {
      Graph<Node, Edge> graph = client.getGenerator().generateFromFiles(filePaths);
      GraphVizExporter.printAsDot(graph);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // TODO: create a root project node if necessary
    try {
      client.getDiffer().computeDiff();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
