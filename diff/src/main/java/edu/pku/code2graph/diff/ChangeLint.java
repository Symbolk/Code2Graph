package edu.pku.code2graph.diff;

import com.github.gumtreediff.actions.ActionClusterFinder;
import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.FileType;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;
import org.apache.log4j.PropertyConfigurator;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ChangeLint {
  private static String repoName = "test_repo";
  private static String repoPath = "/Users/symbolk/coding/data/" + repoName;
  private static String tempDir = "/Users/symbolk/coding/data/temp/c2g";

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

  public static void main(String[] args) throws IOException {

    PropertyConfigurator.configure("log4j.properties");

    // offline: build the graph
    Graph<Node, Edge> graph = offline();
    //      GraphVizExporter.printAsDot(graph);

    String commitID = "";
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(commitID); // together with ground truth

    // gumtree to infer diff nodes
    for (DiffFile diffFile : diffFiles) {
      // filter only xml diff
      if (diffFile.getFileType().equals(FileType.XML)) {
        EditScript es = generateEditScript(diffFile.getAContent(), diffFile.getBContent());
        if (es != null) {
          ActionClusterFinder f = new ActionClusterFinder(es);
          for (Set<Action> cluster : f.getClusters()) {
            cluster.forEach(System.out::println);
          }
        }
      }
    }

    // locate changed xml nodes in the graph

    // query the graph

    // output co-change file/type/method/statement

    // measure accuracy by comparing with ground truth

  }

  private static Graph<Node, Edge> offline() throws IOException {
    // iterate all Java files and match imports
    List<String> allJavaFilePaths = FileUtil.getSpecificFilePaths(repoPath, ".java");

    Map<String, List<String>> rJavaPathsAndImports = new HashMap<>();
    for (String path : allJavaFilePaths) {
      List<String> lines = FileUtil.readFileToLines(path);
      for (String line : lines) {
        line = line.trim();
        // if imports R, collect other imported files
        if (line.startsWith("import") && line.endsWith(".R;")) {
          String packageName = line.replace(".R;", "");
          // filter imported local files
          List<String> imports =
              lines.stream()
                  .map(String::trim)
                  .filter(
                      l ->
                          l.startsWith("import") && l.startsWith(packageName) && !l.endsWith(".R;"))
                  .collect(Collectors.toList());
          String packagePath = convertImportToPath(packageName);
          String parentDir = path.substring(0, path.indexOf(packagePath));
          // get absolute paths
          List<String> importedJavaPaths = new ArrayList<>();
          for (String im : imports) {
            importedJavaPaths.add(parentDir + convertImportToPath(im) + ".java");
          }
          rJavaPathsAndImports.put(path, importedJavaPaths);
        }
      }
    }

    Set<String> filePaths = new HashSet<>();
    for (Map.Entry<String, List<String>> entry : rJavaPathsAndImports.entrySet()) {
      filePaths.add(entry.getKey());
      filePaths.addAll(entry.getValue());
    }

    // collect all xml files (filter only layout?)
    filePaths.addAll(
        FileUtil.getSpecificFilePaths(repoPath, ".xml").stream()
            .filter(path -> path.contains("layout"))
            .collect(Collectors.toList()));

    // construct graph above statement level
    // filter only ASTNodes with R.
    // build cross-lang edges
    //
    Generators generator = Generators.getInstance();

    return generator.generateFromFiles(new ArrayList<>(filePaths));
  }

  private static String convertImportToPath(String importStatement) {
    return importStatement
        .trim()
        .replace("import ", "")
        .replace(";", "")
        .replace(";", "")
        .replace(".", File.separator);
  }

  /**
   * Compute diff and return edit script
   *
   * @param aContent
   * @param bContent
   * @return
   */
  private static EditScript generateEditScript(String aContent, String bContent) {
    //        Run.initGenerators();
    JdtTreeGenerator generator = new JdtTreeGenerator();
    //        Generators generator = Generators.getInstance();

    try {
      TreeContext oldContext = generator.generateFrom().string(aContent);
      TreeContext newContext = generator.generateFrom().string(bContent);
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
