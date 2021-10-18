package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.client.Evaluation;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Extraction {
  private static Logger logger = LoggerFactory.getLogger(Evaluation.class);

  private static String framework = "android";

  // bilibili-android-client BookReader CloudReader XposedInstaller Douya Phonograph LeafPic
  // EhViewer NewPipe AntennaPod
  private static String repoName = "AntennaPod";
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

  public static void main(String[] args)
      throws IOException, ParserConfigurationException, SAXException {
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
        generateMybatisGT();
        break;
    }

    logger.info("Saved groundtruth in file: " + gtPath);
  }

  private static void addCommitIdToPath() {
    String commitID = gitService.getHEADCommitId(repoPath);
    if (commitID != null) {
      gtPath =
          System.getProperty("user.dir")
              + "/client/src/main/resources/"
              + framework
              + "/groundtruth/"
              + repoName
              + ":"
              + commitID.trim()
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

  private static void generateMybatisGT()
      throws IOException, ParserConfigurationException, SAXException {
    MybatisExtractor extractor = new MybatisExtractor();
    extractor.generateInstances(repoPath, repoPath);
    String[] headers = {"SQL", "JAVA"};
    extractor.writeToFile(gtPath, headers);
  }
}