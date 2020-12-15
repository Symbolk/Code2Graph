package edu.pku.code2graph.gen;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;

import java.io.IOException;
import java.nio.charset.Charset;

public abstract class Generator {
  protected abstract Graph<Node, Edge> generate() throws IOException;

  public Configuarator generateFrom() {
    return new Configuarator();
  }

  public class Configuarator {
    private Charset charset = Charset.defaultCharset();

    /**
     * Convert a single source file into a graph
     *
     * @param filePath
     * @return
     */
    public Graph<Node, Edge> file(String filePath) throws IOException {
      return generate();
    }

    public Graph<Node, Edge> commit(String commitID) {
      // compare the commit with its parent
      return null;
    }

    public Graph<Node, Edge> commits(String startCommitID, String endCommitID) {
      return null;
    }

    public Graph<Node, Edge> workingTree() {
      // extract changed/diff files in the working directory
      return null;
    }
  }
}
