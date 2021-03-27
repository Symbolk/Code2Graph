package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.DataCollector;
import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.FileType;
import edu.pku.code2graph.diff.util.DiffUtil;
import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Generators;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.FileUtil;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChangeLint {
  private static String repoName = "LeafPic";
  //  private static String repoName = "test_repo";
  private static String repoPath = "/Users/symbolk/coding/data/repos/" + repoName;
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

    BasicConfigurator.configure();

    // 1. Offline process: given the commit id of the earliest future multi-lang commit
    // checkout to that version
    // offline: build the graph for the current version
    //    Graph<Node, Edge> graph = offline();
    //    GraphVizExporter.printAsDot(graph);

    // 2. Online process: for each of the future commits, extract the changes as GT
    // predict & compare
    String commitID = "ea5ccf3";
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(commitID); // together with ground truth
    DataCollector dataCollector = new DataCollector(tempDir);
    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);

    Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
    Map<String, List<Operation>> javaDiffs = new HashMap<>();

    for (DiffFile diffFile : diffFiles) {
      if (diffFile.getFileType().equals(FileType.XML)) {
        xmlDiffs.put(diffFile.getARelativePath(), computeXMLChanges(tempFilePaths, diffFile));
      } else if (diffFile.getFileType().equals(FileType.JAVA)) {
        javaDiffs.put(
            diffFile.getARelativePath(),
            computeJavaChanges(diffFile.getAContent(), diffFile.getBContent()));
      }
    }

    // locate changed xml nodes in the graph

    // predict co-changes according to context/similar nodes

    // output co-change file/type/method

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
   * Compute diff and return edit script with gumtree spoon
   *
   * @param aContent
   * @param bContent
   * @return
   */
  private static List<Operation> computeJavaChanges(String aContent, String bContent) {
    AstComparator diff = new AstComparator();
    Diff editScript = diff.compare(aContent, bContent);
    List<Operation> operations = editScript.getRootOperations();
    // TODO filter at member/type/file level
    //    for (Operation operation : operations) {
    //      System.out.println(operation);
    //    }
    return operations;
  }

  private static List<XMLDiff> computeXMLChanges(
      Pair<List<String>, List<String>> tempFilePaths, DiffFile diffFile) {
    // find the file path with relative path
    Optional<String> aPath =
        tempFilePaths.getLeft().stream()
            .filter(path -> path.trim().endsWith(diffFile.getARelativePath()))
            .findFirst();
    Optional<String> bPath =
        tempFilePaths.getRight().stream()
            .filter(path -> path.trim().endsWith(diffFile.getBRelativePath()))
            .findFirst();

    String output = "";
    if (aPath.isPresent() && bPath.isPresent()) {
      // compare with system command
      output =
          DiffUtil.runSystemCommand(
              tempDir, Charset.defaultCharset(), "xmldiff", "-p", aPath.get(), bPath.get());
    }
    // TODO process added and deleted files
    // parse the output and collected diff
    Stream<String> lines = output.lines();
    List<XMLDiff> xmlDiffs = new ArrayList<>();
    lines.forEach(line -> xmlDiffs.add(convertXMLDiff(line)));
    return xmlDiffs;
  }

  private static XMLDiff convertXMLDiff(String line) {
    String[] parts = StringUtils.removeEnd(StringUtils.removeStart(line, "["), "]").split(",");
    // convert the diff to objects
    if (parts.length == 4) {
      return new XMLDiff(parts[0], parts[1], parts[2], parts[3]);
    } else if (parts.length == 3) {
      return new XMLDiff(parts[0], parts[1], parts[2]);
    } else if (parts.length == 2) {
      return new XMLDiff(parts[0], parts[1]);
    }
    return null;
  }
}
