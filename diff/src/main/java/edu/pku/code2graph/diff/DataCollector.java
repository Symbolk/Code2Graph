package edu.pku.code2graph.diff;

import edu.pku.code2graph.diff.model.DiffFile;
import edu.pku.code2graph.diff.model.FileType;
import edu.pku.code2graph.diff.model.Version;
import edu.pku.code2graph.util.FileUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Collect the left and right version of diff files as temporary files on disk */
public class DataCollector {

  Logger logger = LoggerFactory.getLogger(DataCollector.class);

  private String tempPath;

  public DataCollector(String tempDir) {
    this.tempPath = tempDir;
  }

  /**
   * Collect the left and right version of diff files
   *
   * @return
   */
  public Pair<List<String>, List<String>> collectForWorkingTree(List<DiffFile> diffFiles) {
    String aDir = tempPath + File.separator + Version.A.asString() + File.separator;
    String bDir = tempPath + File.separator + Version.B.asString() + File.separator;

    return collect(aDir, bDir, diffFiles);
  }

  /**
   * Collect the left and right version of diff files
   *
   * @param commitID
   * @return
   */
  public Pair<List<String>, List<String>> collectForCommit(
      String commitID, List<DiffFile> diffFiles) {
    String aDir =
        tempPath
            + File.separator
            + commitID
            + File.separator
            + Version.A.asString()
            + File.separator;
    String bDir =
        tempPath
            + File.separator
            + commitID
            + File.separator
            + Version.B.asString()
            + File.separator;

    return collect(aDir, bDir, diffFiles);
  }

  /**
   * Collect the diff files into the data dir
   *
   * @param aDir
   * @param bDir
   * @param diffFiles
   * @return
   */
  private Pair<List<String>, List<String>> collect(
      String aDir, String bDir, List<DiffFile> diffFiles) {
    FileUtil.createDir(aDir);
    FileUtil.createDir(bDir);
    List<String> aPaths = new ArrayList<>();
    List<String> bPaths = new ArrayList<>();
    for (DiffFile diffFile : diffFiles) {
      // skip binary files
      if (diffFile.getFileType().equals(FileType.BIN)) {
        continue;
      }
      String aPath = "", bPath = "";
      switch (diffFile.getStatus()) {
        case ADDED:
        case UNTRACKED:
          bPath = bDir + diffFile.getBRelativePath();
          if (FileUtil.writeStringToFile(diffFile.getBContent(), bPath)) {
          } else {
            logger.error("Error when collecting: " + diffFile.getStatus() + ":" + bPath);
          }
          break;
        case DELETED:
          aPath = aDir + diffFile.getARelativePath();
          if (!FileUtil.writeStringToFile(diffFile.getAContent(), aPath)) {
            logger.error("Error when collecting: " + diffFile.getStatus() + ":" + aPath);
          }
          break;
        case MODIFIED:
        case RENAMED:
        case COPIED:
          aPath = aDir + diffFile.getARelativePath();
          bPath = bDir + diffFile.getBRelativePath();
          boolean aOk = FileUtil.writeStringToFile(diffFile.getAContent(), aPath);
          boolean bOk = FileUtil.writeStringToFile(diffFile.getBContent(), bPath);
          if (!aOk || !bOk) {
            logger.error("Error when collecting: " + diffFile.getStatus() + ":" + aPath);
          }
          break;
      }
      if (!aPath.isEmpty()) {
        aPaths.add(aPath);
      }
      if (!bPath.isEmpty()) {
        bPaths.add(bPath);
      }
    }
    return Pair.of(aPaths, bPaths);
  }
}
