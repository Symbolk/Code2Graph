package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.RepoAnalyzer;
import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import edu.pku.code2graph.diff.util.MetricUtil;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Range;
import edu.pku.code2graph.util.GraphUtil;
import edu.pku.code2graph.xll.Link;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Evaluation {
  private static Logger logger = LoggerFactory.getLogger(Evaluation.class);

  // test one repo at a time
  private static String framework = "springmvc";
  private static String repoName = "sagan";
  private static String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;
  private static String configPath =
      System.getProperty("user.home")
          + "/coding/dev/Code2Graph/client/src/main/resources/android/config.yml";
  //      FileUtil.getPathFromURL(
  //          Evaluation.class.getClassLoader().getResource(framework + "/config.yml"));

  private static Code2Graph client = null;
  private static List<Link> xllLinks = new ArrayList<>();

  private static GitService gitService = new GitServiceCGit();
  private static RepoAnalyzer repoAnalyzer = new RepoAnalyzer(repoName, repoPath);

  public static void main(String[] args) {
    BasicConfigurator.configure();
//    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    // set up
    client = new Code2Graph(repoName, repoPath, configPath);
    client.setSupportedLanguages(
        new HashSet(Arrays.asList(Language.JAVA, Language.HTML, Language.XML, Language.SQL)));

    logger.info("Generating graph for repo: " + repoName);
    Graph<Node, Edge> graph = client.generateGraph();
    xllLinks = client.getXllLinks();
    // export to csv files

    // run evaluation
    //    testXLLDetection();
    testCochange();
  }

  /** Run the experiments on real repo, and compare the results with the ground truth */
  private static void testXLLDetection() {
    // load ground truth by reading csv
    Set<Link> groundTruth = new HashSet<Link>();

    // compare
    Set<Link> output = new HashSet<>(xllLinks);
    int intersectionNum = MetricUtil.intersectSize(groundTruth, output);

    // compute precision/recall
    double precision = MetricUtil.computeProportion(intersectionNum, output.size());
    double recall = MetricUtil.computeProportion(intersectionNum, groundTruth.size());

    logger.info("Precision = " + precision);
    logger.info("Recall = " + recall);
  }

  /** Metric: compare output with ground truth and calculate the precision and recall */
  private static Pair<Double, Double> computePR() {
    return Pair.of(1.0, 1.0);
  }

  /** Rely on identifier transformation */
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
