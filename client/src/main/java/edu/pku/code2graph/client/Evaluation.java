package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.util.MetricUtil;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.xll.Link;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Evaluation {
  // test one repo at a time
  private static String repoName = "";
  private static String repoPath = "";
  private static Code2Graph client = null;

  public static void main(String[] args) {
    // set up
    repoName = "";
    repoPath = "";
    client = new Code2Graph(repoName, repoPath, "");
    client.setSupportedLanguages(
        new HashSet(Arrays.asList(Language.JAVA, Language.HTML, Language.XML)));

    // run evaluation
    testXLLDetection();
  }

  /** Run the experiments on real repo, and compare the results with the ground truth */
  private static void testXLLDetection() {
    // load ground truth by reading csv
    Set<Link> groundTruth = new HashSet<Link>();

    // detect xll
    Graph<Node, Edge> graph = client.generateGraph();
    System.out.println(graph.vertexSet().size());
    System.out.println(graph.edgeSet().size());
    List<Link> xllLinks = client.getXllLinks();

    // compare
    Set<Link> output = new HashSet<>(xllLinks);
    int intersectionNum = MetricUtil.intersectSize(groundTruth, output);

    // compute precision/recall
    double precision = MetricUtil.computeProportion(intersectionNum, output.size());
    double recall = MetricUtil.computeProportion(intersectionNum, groundTruth.size());

    System.out.println("Precision = " + precision);
    System.out.println("Recall = " + recall);
  }

  /** Metric: compare output with ground truth and calculate the precision and recall */
  private static Pair<Double, Double> computePR() {
    return Pair.of(1.0, 1.0);
  }

  /**
   * Rely on identifier transformation
   */
  private static void testRename() {
    // randomly sample 20% named xll

    //  pick a uri to rename

    //  client.crossLanguageRename()

    // compare, check or evaluate
  }

  /**
   * Co-change file prediction
   * how to reduce noise? (xml --> java --> java, java)
   */
  private static void testCochange() {
    // 1. for recent 20 historical multi-lang commits --> predict

    // 2. sample 20%, find relevant commits --> filter  --> predict

    // given changes in lang A, mask changes in lang B

    // predict co-changes in B according to code coupling

  }

  /**
   * Need to find 10-20 historical inconsistency-fixing commits
   */
  private static void testLint() {
    // for the previous version (parent commit) of an cross-language inconsistency fixing commit
    // 1. filter by keywords (miss/close/fix/resolve) --> find bug-introducing --> check multilingual

    // 2. filter by diff size (#diff hunks<k & #diff files<n) --> 20% of all commits --> check if bug-fixing

    // 3.  given the output of cochange -> check later modification commits  --> check if made to fix cross-lang breaking

    // output problems (possible but not co-changed)

    // manually check whether is a known/unknown/not a bug/code smell fix
  }
}
