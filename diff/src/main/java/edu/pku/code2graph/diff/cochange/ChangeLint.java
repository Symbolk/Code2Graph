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
import org.apache.log4j.PropertyConfigurator;
import org.atteo.classindex.ClassIndex;
import org.jgrapht.Graph;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtTypeMember;
import spoon.support.reflect.declaration.CtTypeImpl;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static edu.pku.code2graph.gen.xml.cochange.XMLDiffUtil.computeXMLChanges;

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

    //    BasicConfigurator.configure();
    PropertyConfigurator.configure(
        System.getProperty("user.dir") + File.separator + "log4j.properties");

    // 1. Offline process: given the commit id of the earliest future multi-lang commit
    // checkout to that version
    //    DiffUtil.runSystemCommand(repoPath, Charset.defaultCharset(), "git", "checkout", "-b",
    // "changelint", "e457da8");
    //    offline: build the graph for the current version
    //    Graph<Node, Edge> graph = buildGraph();
    //    System.out.println(graph.vertexSet().size());
    //    System.out.println(graph.edgeSet().size());
    //    GraphVizExporter.copyAsDot(graph);

    // 2. Online process: for each of the future commits, extract the changes as GT
    // predict & compare
    String testCommitID = "ea5ccf3";
    RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);
    List<DiffFile> diffFiles = repoAnalyzer.analyzeCommit(testCommitID);
    DataCollector dataCollector = new DataCollector(tempDir);
    Pair<List<String>, List<String>> tempFilePaths = dataCollector.collect(diffFiles);

    // file relative path, changed xml element id
    Map<String, List<XMLDiff>> xmlDiffs = new HashMap<>();
    // file relative path, type name, member name
    Map<String, Set<Pair<String, String>>> javaDiffs = new HashMap<>();

    for (DiffFile diffFile : diffFiles) {
      if (diffFile.getFileType().equals(FileType.XML)) {
        xmlDiffs.put(diffFile.getARelativePath(), computeXMLChanges(tempFilePaths, diffFile));
      } else if (diffFile.getFileType().equals(FileType.JAVA)) {
        javaDiffs.put(diffFile.getARelativePath(), computeJavaChanges(diffFile));
      }
    }

    // locate changed xml nodes in the graph
    for (Map.Entry<String, List<XMLDiff>> entry : xmlDiffs.entrySet()) {
      for (XMLDiff diff : entry.getValue()) {
        if (diff.getName() != null) {
          if (diff.getName().startsWith("@+id")) {
            System.out.println(diff);
          }
        }
      }
    }
    // modified

    // removed

    // added: predict co-changes according to context/similar nodes

    // output co-change file/type/member

    // measure accuracy by comparing with ground truth

  }

  private static Graph<Node, Edge> buildGraph() throws IOException {
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
   * @return
   */
  private static Set<Pair<String, String>> computeJavaChanges(DiffFile diffFile) {
    Set<Pair<String, String>> results = new HashSet<>();

    AstComparator diff = new AstComparator();
    Diff editScript = diff.compare(diffFile.getAContent(), diffFile.getBContent());
    // build GT: process Java changes to file/type/member
    for (Operation operation : editScript.getRootOperations()) { // or allOperations?
      Pair<String, String> names = findWrappedTypeAndMemberNames(operation.getSrcNode());
      results.add(names);
    }
    return results;
  }

  private static Pair<String, String> findWrappedTypeAndMemberNames(CtElement element) {
    if (element instanceof CtTypeImpl) {
      return Pair.of(((CtTypeImpl) element).getQualifiedName(), "");
    } else if (element instanceof CtTypeMember) {
      CtTypeMember member = (CtTypeMember) element;
      return Pair.of(member.getDeclaringType().getQualifiedName(), member.getSimpleName());
    } else {
      // find parent member and type
      CtElement parentType = element.getParent();
      CtElement parentMember = element.getParent();

      while (parentType != null && !(parentType instanceof CtTypeImpl)) {
        parentType = parentType.getParent();
      }
      while (parentMember != null && !(parentMember instanceof CtTypeMember)) {
        parentMember = parentMember.getParent();
      }

      String parentTypeName =
          parentType != null ? ((CtTypeImpl) parentType).getQualifiedName() : "";
      String parentMemberName =
          parentMember != null ? ((CtTypeMember) parentMember).getSimpleName() : "";
      return Pair.of(parentTypeName, parentMemberName);
    }
    //    String nodeType = element.getClass().getSimpleName();
    //    nodeType = nodeType.substring(2, nodeType.length() - 4);
  }
}
