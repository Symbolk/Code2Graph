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
     * Generate graph from a set of files
     * @param filePaths
     * @return
     * @throws IOException
     */
    public Graph<Node, Edge> files(List<String> filePaths) throws IOException {
      return generate(filePaths);
    }
  }
}
