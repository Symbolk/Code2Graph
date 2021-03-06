package edu.pku.code2graph.gen;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.atteo.classindex.IndexSubclasses;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@IndexSubclasses
public abstract class Generator {
  protected Logger logger = LoggerFactory.getLogger(Generator.class);

  protected abstract Graph<Node, Edge> generate(List<String> filePaths) throws IOException;

  public Configurator generateFrom() {
    return new Configurator();
  }

  public class Configurator {
    private Charset charset = Charset.defaultCharset();

    /**
     * Generate graph from a set of files
     *
     * @param filePaths
     * @return
     * @throws IOException
     */
    public Graph<Node, Edge> files(List<String> filePaths) throws IOException {
      return generate(filePaths);
    }
  }
}
