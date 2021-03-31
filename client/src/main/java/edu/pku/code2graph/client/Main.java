package edu.pku.code2graph.client;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.antlr3.xml.XmlTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import edu.pku.code2graph.diff.Differ;
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
  private static String tempDir = "/Users/symbolk/coding/data/temp/c2g";

  public static void main(String[] args) {
    // config the logger with log4j
    //    System.setProperty("logs.dir", System.getProperty("user.dir"));
    PropertyConfigurator.configure("log4j.properties");
    //    // use basic configuration when packaging
    //    BasicConfigurator.configure();
    //    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
    testDiff();
  }

  private static void testDiff() {
    Code2Graph client =
        new Code2Graph("MLManager", "/Users/symbolk/coding/data/repos/MLManager", tempDir);

    // TODO: create a root project node if necessary
    try {
      Differ differ = client.getDiffer();
      // TODO hide concrete method calls for diff as one public API
      differ.buildGraphs("76adb20");
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
    try {
      Graph<Node, Edge> graph = client.getGenerator().generateFromFiles(filePaths);
      GraphVizExporter.printAsDot(graph);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Compute diff and return edit script
   *
   * @param oldContent
   * @param newContent
   * @return
   */
  private static EditScript computeXMLChangesWithGumtree(String oldContent, String newContent) {
    //        Generators generator = Generators.getInstance();
    TreeGenerator generator = new XmlTreeGenerator();
    try {
      TreeContext oldContext = generator.generateFrom().string(oldContent);
      TreeContext newContext = generator.generateFrom().string(newContent);
      Matcher matcher = Matchers.getInstance().getMatcher();

      MappingStore mappings = matcher.match(oldContext.getRoot(), newContext.getRoot());
      EditScript editScript = new ChawatheScriptGenerator().computeActions(mappings);
      return editScript;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
