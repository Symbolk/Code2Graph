package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.client.Evaluation;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;

import java.io.IOException;

public class Extraction {
  private static Logger logger = LoggerFactory.getLogger(Evaluation.class);

  private static String framework = "android";
  private static String repoName = "NewPipe";
  private static String repoPath =
      System.getProperty("user.home") + "/coding/xll/" + framework + "/" + repoName;

  private static String gtPath =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/groundtruth/"
          + repoName
          + ".csv";

  private static GitService gitService = new GitServiceCGit();

  public static void main(String[] args) throws IOException {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    addCommitIdToPath();

    logger.info("Generating groundtruth for repo: " + repoName);

    switch (framework) {
      case "android":
        generateAndroidGT();
        break;
      case "springmvc":
        generateSpringGT();
        break;
      case "mybatis":
    }
  }

  private static void addCommitIdToPath() {
    String commitId = gitService.getCommitId(repoPath);
    if (commitId != null) {
      gtPath =
          System.getProperty("user.dir")
              + "/client/src/main/resources/"
              + framework
              + "/groundtruth/"
              + repoName
              + ":"
              + commitId.trim()
              + ".csv";
    }
  }

  private static void generateSpringGT() throws IOException {
    SpringExtractor extractor = new SpringExtractor();
    extractor.generateInstances(repoPath, repoPath);
    String[] headers = {"HTML", "JAVA"};
    extractor.writeToFile(gtPath, headers);
  }

  private static void generateAndroidGT() throws IOException {
    AndroidExtractor extractor = new AndroidExtractor();
    extractor.generateInstances(repoPath, repoPath);
    String[] headers = {"XML", "JAVA"};
    extractor.writeToFile(gtPath, headers);
  }
}
