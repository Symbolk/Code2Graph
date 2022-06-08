package edu.pku.code2graph.client;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.util.GitService;
import edu.pku.code2graph.diff.util.GitServiceCGit;
import edu.pku.code2graph.util.FileUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static edu.pku.code2graph.client.CacheHandler.initCache;
import static edu.pku.code2graph.client.CacheHandler.updateCache;

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
      String initCommit = commits.get(0).replace("\"", "");
      if(!commits.isEmpty())
        initCacheForCommit(commits.get(0).replace("\"", ""));
      int size = commits.size() - 1;
      for (int i = 0; i < size; i++) {
        initCacheForCommitByUpdate(
            commits.get(i).replace("\"", ""), commits.get(i + 1).replace("\"", ""));
      }
      gitService.checkoutByLongCommitID(initCommit);
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

    initCache(framework, repoPath, Paths.get(cacheDir, commit).toString());
  }

  public static void initCacheForCommitByUpdate(String commitA, String commitB)
      throws IOException, ParserConfigurationException, SAXException {
    if (!gitService.checkoutByLongCommitID(commitB)) {
      logger.error("Failed to checkout to {}", commitB);
      return;
    } else {
      logger.info("Successfully checkout to {}", commitB);
    }

    String cacheA = Paths.get(cacheDir, commitA).toString(),
        cacheB = Paths.get(cacheDir, commitB).toString();
    FileUtil.clearDir(cacheB);
    FileUtil.copyDir(cacheA, cacheB);

    List<DiffFile> diffFiles = gitService.getChangedFilesAtCommit(commitA);
    for (DiffFile file : diffFiles) {
      if (!file.getARelativePath().isEmpty()) {
        updateCache(
            framework, repoPath, file.getARelativePath(), Paths.get(cacheDir, commitB).toString());
      }
      if (!file.getBRelativePath().isEmpty()
          && !file.getBRelativePath().equals(file.getARelativePath())) {
        updateCache(
            framework, repoPath, file.getBRelativePath(), Paths.get(cacheDir, commitB).toString());
      }
    }
  }
}
