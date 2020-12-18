package edu.pku.code2graph.gen;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.apache.logging.log4j.core.util.FileUtils;
import org.atteo.classindex.IndexSubclasses;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@IndexSubclasses
public abstract class Generator {
  protected abstract Graph<Node, Edge> generate(List<String> filePaths) throws IOException;

  public Configurator generateFrom() {
    return new Configurator();
  }

  public class Configurator {
    private Charset charset = Charset.defaultCharset();

    /**
     * Convert a single source file into a graph
     *
     * @param filePath the absolute file path
     * @return
     */
    public Graph<Node, Edge> file(String filePath) throws IOException {
      return generate(Arrays.asList(filePath));
    }

//    public Graph<Node, Edge> folder(String folderPath) throws IOException {
//      Collection<File> javaFiles = FileUtils.listFiles(new File(srcDir), new String[] {"java"}, true);
//      return generate();
//    }

    /**
     * Generate graph from a set of files
     * @param filePaths
     * @return
     * @throws IOException
     */
    public Graph<Node, Edge> files(List<String> filePaths) throws IOException {
      return generate(filePaths);
    }

    /**
     * Generate the change graph between a commit and its parent (maybe in another subproject?)
     * @param commitID
     * @return
     * @throws IOException
     */
    public Graph<Node, Edge> commit(String commitID) throws IOException {
      // compare the commit with its parent

      // get the diff files and old/new versions

      // collect the content and saves to temp dir (optional)
      List<String> filePaths = new ArrayList<>();
      // pass the file paths/content to the generator

      return generate(filePaths);
    }

    public Graph<Node, Edge> commits(String startCommitID, String endCommitID) throws IOException {
      // extract changed/diff files in the working directory

      // collect the content and saves to temp dir

      // pass the file paths/content to the generator

      List<String> filePaths = new ArrayList<>();
      // pass the file paths/content to the generator

      return generate(filePaths);
    }

    public Graph<Node, Edge> workingTree() throws IOException {
      // extract changed/diff files in the working directory

      // collect the content and saves to temp dir

      // pass the file paths/content to the generator

      List<String> filePaths = new ArrayList<>();
      // pass the file paths/content to the generator

      return generate(filePaths);
    }
  }
}
