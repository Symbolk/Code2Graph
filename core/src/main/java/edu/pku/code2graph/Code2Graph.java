package edu.pku.code2graph;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atteo.classindex.ClassIndex;

public class Code2Graph {
  private static final Logger LOGGER = LogManager.getLogger();
  private String repoName;
  private String repoPath;

  public Code2Graph(String repoName, String repoPath) {
    this.repoName = repoName;
    this.repoPath = repoPath;
  }

  static {
    initGenerators();
  }

  public static void initGenerators() {
    ClassIndex.getSubclasses(Generator.class)
        .forEach(
            gen -> {
              Register a = gen.getAnnotation(Register.class);
              if (a != null) Generators.getInstance().install(gen, a);
            });
  }
}
