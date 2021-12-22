package edu.pku.code2graph.client;

import com.csvreader.CsvReader;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Test on the simulated code smell/side effects of code changes */
public class LintTest {
  public static void main(String[] args) throws IOException {
    String framework = "android";
    String repoName = "CloudReader";
    String configFilePath =
        System.getProperty("user.dir") + "/client/src/main/resources/" + framework + "/config.yml";

    String basePath = System.getProperty("user.home") + "/Downloads/lint/" + "/";
    String version1 = basePath + repoName + "0";
    String version2 = basePath + repoName;
    System.out.println("V1 at: " + version1);
    System.out.println("V2 at: " + version2);

    List<Link> res = lint(configFilePath, version1, version2);
    res.forEach(System.out::println);
    //  compare with the ground truth list, compute the precision for current case
    String gtPath = basePath + "_gt.csv";
    CsvReader gtReader = new CsvReader(gtPath);
    gtReader.readHeaders();
    String[] gtHeaders = gtReader.getHeaders();

    Set<String> gtLines = new HashSet<>();
    String leftLang = gtHeaders[0], rightLang = gtHeaders[1];
    while (gtReader.readRecord()) {
      gtLines.add(gtReader.get(leftLang) + "," + gtReader.get(rightLang));
    }
    gtReader.close();

    // compute precision for the current test case

  }

  private static List<Link> lint(String configFilePath, String repoPath, String dirtyRepoPath) {
    // collect all URIs as a tree in v1
    URITree tree1 = GraphUtil.getUriTree();
    URITree tree2 = GraphUtil.getUriTree();
    // analyze volation in the current change
    List<Link> res = analyzeViolation(configFilePath, tree1, tree2);
    return res;
  }

  private static List<Link> analyzeViolation(String configFilePath, URITree tree1, URITree tree2) {
    // analyze XLL in tree1
    // compare tree1 with tree2
    // return broken XLL
    return new ArrayList<>();
  }
}
