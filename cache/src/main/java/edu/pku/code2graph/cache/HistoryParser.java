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
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

import static edu.pku.code2graph.cache.CacheHandler.getCacheSHA;
import static edu.pku.code2graph.cache.CacheHandler.initCache;

public class HistoryParser {
  private static Logger logger = LoggerFactory.getLogger(HistoryParser.class);

  public static Boolean useCheckout = true;
  public static String framework = "android";
  private static String repoName = "NewPipe";
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

  private static List<String> commits;
  private static BufferedWriter writer;

  public static void main(String[] args) {
    init();

    try {
      commits = gitService.getCommitHistory();
      if (!useCheckout) {
        FileUtil.copyDir(repoPath, tmpPath);
      } else {
        tmpPath = repoPath;
      }
      String initCommit = "";

      File commitList = new File(commitListPath);
      if (!commitList.exists()) FileUtil.createFile(commitListPath);
      System.out.println(commitListPath);
      writer = new BufferedWriter(new FileWriter(commitList));
      for (int i = 0; i < commits.size(); i++) {
        initCacheForCommit(i);
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

  public static void initCacheForCommit(int index)
      throws IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException {
    writer.write(commits.get(index));
    String commit = commits.get(index);
    if (useCheckout && !gitService.checkoutByLongCommitID(commit)) {
      logger.error("Failed to checkout to {}", commit);
      writer.write("\n");
      return;
    } else if (useCheckout) {
      String progress = new DecimalFormat("0.00%").format((double) index / commits.size());
      logger.info("Successfully checkout to {} ({})", commit, progress);
    }

    if (index == 0) {
      initCache(framework, tmpPath, cacheDir, true);
    } else {
      if (!useCheckout) logger.info("store uri tree for {}", commit);

      List<DiffFile> diffFiles = gitService.getChangedFilesAtCommit(commit);
      for (DiffFile file : diffFiles) {
        if (!file.getARelativePath().isEmpty()) {
          if (!useCheckout) {
            String pathA = file.getARelativePath();
            String fileContent = gitService.getFileAtCommit(pathA, commit);
            overwriteOrDelete(Paths.get(tmpPath, pathA).toString(), fileContent);
          }
          CacheHandler.initCache(framework, tmpPath, file.getARelativePath(), cacheDir, true);
        }
        if (!file.getBRelativePath().isEmpty()
            && !file.getBRelativePath().equals(file.getARelativePath())) {
          if (!useCheckout) {
            String pathB = file.getBRelativePath();
            String fileContent = gitService.getFileAtCommit(pathB, commit);
            overwriteOrDelete(Paths.get(tmpPath, pathB).toString(), fileContent);
          }
          CacheHandler.initCache(framework, tmpPath, file.getBRelativePath(), cacheDir, true);
        }
      }
    }

    if (useCheckout) {
      Collection<String> hashes = getCacheSHA(framework, repoPath, cacheDir);
      for (String hash : hashes) {
        writer.write("," + hash);
      }
    }
    writer.write("\n");
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
