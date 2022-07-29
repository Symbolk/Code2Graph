package edu.pku.code2graph.cache;

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
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;

import static edu.pku.code2graph.cache.CacheHandler.getCacheSHA;
import static edu.pku.code2graph.cache.CacheHandler.initCache;

public class HistoryParser {
  private static Logger logger = LoggerFactory.getLogger(HistoryParser.class);

  public static Boolean useCheckout = true;
  public static String framework = "android";
  private static String repoName = "CloudReader";
  private static String repoPath =
      System.getProperty("user.dir")
          + "/cache/src/main/resources/"
          + framework
          + "/repos/"
          + repoName;
  private static String cacheDir =
      System.getProperty("user.home") + "/coding/xll/sha-history/" + framework + "/" + repoName;
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
      if (!commitList.exists()) FileUtil.createFile(commitListPath);
      BufferedWriter writer = new BufferedWriter(new FileWriter(commitList));

      int size = commits.size() - 1;
      if (!commits.isEmpty()) initCommit = commits.get(size).replace("\"", "");
      writer.write(initCommit);
      Collection<String> hashes = initCacheForCommit(initCommit);
      if (hashes != null) {
        for (String hash : hashes) {
          writer.write("," + hash);
        }
      }
      writer.write("\n");

      for (int i = size - 1; i >= 0; i--) {
        writer.write(commits.get(i).replace("\"", ""));
        hashes = initCacheForCommitByUpdate(commits.get(i).replace("\"", ""));
        if (hashes != null) {
          for (String hash : hashes) {
            writer.write("," + hash);
          }
        }
        writer.write("\n");
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

  public static Collection<String> initCacheForCommit(String commit)
      throws IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException {
    if (useCheckout && !gitService.checkoutByLongCommitID(commit)) {
      logger.error("Failed to checkout to {}", commit);
      return null;
    } else if (useCheckout) {
      logger.info("Successfully checkout to {}", commit);
    }

    initCache(framework, tmpPath, cacheDir, true);

    if (!useCheckout) return null;
    return getCacheSHA(framework, repoPath, cacheDir);
  }

  public static Collection<String> initCacheForCommitByUpdate(String commitA)
      throws IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException {
    if (useCheckout && !gitService.checkoutByLongCommitID(commitA)) {
      logger.error("Failed to checkout to {}", commitA);
      return null;
    } else if (useCheckout) {
      logger.info("Successfully checkout to {}", commitA);
    }

    if (!useCheckout) logger.info("store uritree for {}", commitA);

    List<DiffFile> diffFiles = gitService.getChangedFilesAtCommit(commitA);
    for (DiffFile file : diffFiles) {
      if (!file.getARelativePath().isEmpty()) {
        if (!useCheckout) {
          String pathA = file.getARelativePath();
          String fileContent = gitService.getFileAtCommit(pathA, commitA);
          overwriteOrDelete(Paths.get(tmpPath, pathA).toString(), fileContent);
        }
        CacheHandler.initCache(framework, tmpPath, file.getARelativePath(), cacheDir, true);
      }
      if (!file.getBRelativePath().isEmpty()
          && !file.getBRelativePath().equals(file.getARelativePath())) {
        if (!useCheckout) {
          String pathB = file.getBRelativePath();
          String fileContent = gitService.getFileAtCommit(pathB, commitA);
          overwriteOrDelete(Paths.get(tmpPath, pathB).toString(), fileContent);
        }
        CacheHandler.initCache(framework, tmpPath, file.getBRelativePath(), cacheDir, true);
      }
    }

    if (!useCheckout) return null;
    return getCacheSHA(framework, repoPath, cacheDir);
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
