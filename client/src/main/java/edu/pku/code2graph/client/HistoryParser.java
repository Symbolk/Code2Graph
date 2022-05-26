package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

import static edu.pku.code2graph.client.CacheHandler.initCache;

public class HistoryParser {
  private static Logger logger = LoggerFactory.getLogger(HistoryParser.class);

  public static String framework = "android";
  private static String repoName = "CloudReader";
  private static String repoPath =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/repos/"
          + repoName;
  private static String cacheDir =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/history/"
          + repoName;

  private static GitService gitService;

  public static void main(String[] args) {
    init();

    try {
      List<String> commits = gitService.getCommitHistory();
      for (String commit : commits) {
        initCacheForCommit(commit.replace("\"", ""));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void init() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
    try {
      gitService = new GitServiceCGit(repoPath);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void initCacheForCommit(String commit)
      throws IOException, ParserConfigurationException, SAXException {
    if (!gitService.checkoutByLongCommitID(commit)) {
      logger.error("Failed to checkout to {}", commit);
      return;
    } else {
      logger.info("Successfully checkout to {}", commit);
    }

    initCache(framework, repoPath, cacheDir + "/" + commit);
  }
}
