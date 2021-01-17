package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.Differ;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import org.atteo.classindex.ClassIndex;

/** Java API client */
public class Code2Graph {
  // meta info
  private String repoName;
  private String repoPath;

  // components
  private Generators generator;
  private Differ differ;

  // options
  private boolean involveImported = false; // consider files imported by diff files or not
  private boolean limitToSource = true; // limit the node inside collected source files or not

  public Code2Graph(String repoName, String repoPath) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.differ = new Differ(repoName, repoPath);
    this.generator = Generators.getInstance();
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

  public String getRepoName() {
    return repoName;
  }

  public String getRepoPath() {
    return repoPath;
  }

  public Generators getGenerator() {
    return generator;
  }

  public Differ getDiffer() {
    return differ;
  }
}
