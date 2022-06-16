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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static edu.pku.code2graph.client.CacheHandler.initCache;
import static edu.pku.code2graph.client.CacheHandler.updateCache;

public class HistoryParser {
  private static Logger logger = LoggerFactory.getLogger(HistoryParser.class);

  public static Boolean useCheckout = true;
  public static String framework = "android";
  private static String repoName = "CloudReader";
  private static String repoPath =
      System.getProperty("user.dir")
          + "/client/src/main/resources/"
          + framework
          + "/repos/"
          + repoName;
  private static String cacheDir =
      System.getProperty("user.home") + "/coding/xll/history/" + framework + "/" + repoName;
  private static String tmpPath =
      System.getProperty("user.home") + "/coding/xll/tmp/" + framework + "/" + repoName;
  private static String commitListPath = cacheDir + "/commits.txt";
  private static GitService gitService;

  public static void main(String[] args) {
    init();

    try {
      List<String> commits = gitService.getCommitHistory();
      if (!useCheckout) {
        FileUtil.copyDir(repoPath, tmpPath);
      } else {
        tmpPath = repoPath;
      }
      String initCommit = "";

      File commitList = new File(commitListPath);
      BufferedWriter writer = new BufferedWriter(new FileWriter(commitList));
      if (!commits.isEmpty()) initCommit = commits.get(0).replace("\"", "");
      writer.write(initCommit + "\n");
      if (!initCommit.isEmpty()) initCacheForCommit(initCommit);
      int size = commits.size() - 1;
      for (int i = 0; i < size; i++) {
        writer.write(commits.get(i + 1).replace("\"", "") + "\n");
        initCacheForCommitByUpdate(
            commits.get(i).replace("\"", ""), commits.get(i + 1).replace("\"", ""));
      }
      writer.close();
      if (useCheckout) gitService.checkoutByLongCommitID(initCommit);
      else {
        File file = new File(tmpPath);
        file.delete();
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
    if (useCheckout && !gitService.checkoutByLongCommitID(commit)) {
      logger.error("Failed to checkout to {}", commit);
      return;
    } else if (useCheckout) {
      logger.info("Successfully checkout to {}", commit);
    }

    initCache(framework, tmpPath, Paths.get(cacheDir, commit).toString());
  }

  public static void initCacheForCommitByUpdate(String commitA, String commitB)
      throws IOException, ParserConfigurationException, SAXException {
    if (useCheckout && !gitService.checkoutByLongCommitID(commitB)) {
      logger.error("Failed to checkout to {}", commitB);
      return;
    } else if (useCheckout) {
      logger.info("Successfully checkout to {}", commitB);
    }

    if (!useCheckout) logger.info("store uritree for {}", commitB);

    String cacheA = Paths.get(cacheDir, commitA).toString(),
        cacheB = Paths.get(cacheDir, commitB).toString();
    FileUtil.clearDir(cacheB);
    FileUtil.copyDir(cacheA, cacheB);

    List<DiffFile> diffFiles = gitService.getChangedFilesAtCommit(commitA);
    for (DiffFile file : diffFiles) {
      if (!file.getARelativePath().isEmpty()) {
        if (!useCheckout) {
          String pathA = file.getARelativePath();
          String fileContent = gitService.getFileAtCommit(pathA, commitB);
          overwriteOrDelete(Paths.get(tmpPath, pathA).toString(), fileContent);
        }
        updateCache(
            framework, tmpPath, file.getARelativePath(), Paths.get(cacheDir, commitB).toString());
      }
      if (!file.getBRelativePath().isEmpty()
          && !file.getBRelativePath().equals(file.getARelativePath())) {
        if (!useCheckout) {
          String pathB = file.getBRelativePath();
          String fileContent = gitService.getFileAtCommit(pathB, commitB);
          overwriteOrDelete(Paths.get(tmpPath, pathB).toString(), fileContent);
        }
        updateCache(
            framework, tmpPath, file.getBRelativePath(), Paths.get(cacheDir, commitB).toString());
      }
    }
  }

  private static void overwriteOrDelete(String filePath, String fileContent) throws IOException {
    File file = new File(filePath);
    if (!fileContent.equals("")) {
      if (!file.exists()) {
        FileUtil.createFile(filePath);
      }
      BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false));
      writer.write(fileContent);
      writer.close();
    } else {
      File fileA = new File(filePath);
      fileA.delete();
    }
  }
}
