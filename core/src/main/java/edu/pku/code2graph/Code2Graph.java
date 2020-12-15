package edu.pku.code2graph;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;

import java.nio.charset.Charset;

public class Code2Graph {
  private static final Logger LOGGER = LogManager.getLogger();
  private String repoName;
  private String repoPath;

  public Code2Graph(String repoName, String repoPath) {
    this.repoName = repoName;
    this.repoPath = repoPath;
  }

  public Generator getGenerator(Language language) {
//    return new JdtGenerator();
    return null;
  }
}
