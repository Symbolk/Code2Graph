package edu.pku.code2graph.diff.model;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiffFile {
  private String repoID;
  private String repoName;
  private String fileID;
  private Charset charset;
  private Integer index; // the index of the diff file in the current repo, start from 0
  private FileStatus status;
  private FileType fileType;
  private String baseRelativePath;
  private String currentRelativePath;
  private String baseContent;
  private String currentContent;
  private String description;
  private Map<String, DiffHunk> diffHunksMap;
  private transient List<DiffHunk> diffHunks;
  // lines from the raw output of git-diff (for patch generation)
  private List<String> rawHeaders = new ArrayList<>();

  public DiffFile(
      Integer index,
      FileStatus status,
      FileType fileType,
      Charset charset,
      String baseRelativePath,
      String currentRelativePath,
      String baseContent,
      String currentContent) {
    this.index = index;
    this.status = status;
    this.fileType = fileType;
    this.charset = charset;
    this.baseRelativePath = baseRelativePath;
    this.currentRelativePath = currentRelativePath;
    this.baseContent = baseContent;
    this.currentContent = currentContent;
    this.description = status.label;
    this.diffHunks = new ArrayList<>();
    this.diffHunksMap = new HashMap<>();
  }

  /** Constructor to clone object for json serialization */
  public DiffFile(
      String repoID,
      String repoName,
      String fileID,
      Integer index,
      FileStatus status,
      FileType fileType,
      String baseRelativePath,
      String currentRelativePath,
      String baseContent,
      String currentContent,
      Map<String, DiffHunk> diffHunksMap) {
    this.repoID = repoID;
    this.repoName = repoName;
    this.fileID = fileID;
    this.index = index;
    this.status = status;
    this.description = status.label;
    this.fileType = fileType;
    this.baseRelativePath = baseRelativePath;
    this.currentRelativePath = currentRelativePath;
    this.baseContent = baseContent;
    this.currentContent = currentContent;
    this.diffHunksMap = diffHunksMap;
  }

  public String getRepoID() {
    return repoID;
  }

  public String getRepoName() {
    return repoName;
  }

  public String getFileID() {
    return fileID;
  }

  public Charset getCharset() {
    return charset;
  }

  public void setRepoID(String repoID) {
    this.repoID = repoID;
  }

  public void setRepoName(String repoName) {
    this.repoName = repoName;
  }

  public void setFileID(String fileID) {
    this.fileID = fileID;
  }

  public List<String> getRawHeaders() {
    return rawHeaders;
  }

  public void setRawHeaders(List<String> rawHeaders) {
    this.rawHeaders = rawHeaders;
  }

  public FileStatus getStatus() {
    return status;
  }

  public FileType getFileType() {
    return fileType;
  }

  public String getBaseRelativePath() {
    return baseRelativePath;
  }

  public String getCurrentRelativePath() {
    return currentRelativePath;
  }

  public String getRelativePathOf(Version version) {
    if (version.equals(Version.LEFT)) {
      return getBaseRelativePath();
    } else if (version.equals(Version.RIGHT)) {
      return getCurrentRelativePath();
    }
    return "";
  }

  public String getBaseContent() {
    return baseContent;
  }

  public String getCurrentContent() {
    return currentContent;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public Integer getIndex() {
    return index;
  }

  public List<DiffHunk> getDiffHunks() {
    return diffHunks;
  }

  public Map<String, DiffHunk> getDiffHunksMap() {
    return diffHunksMap;
  }

  public void setDiffHunksMap(Map<String, DiffHunk> diffHunksMap) {
    this.diffHunksMap = diffHunksMap;
  }

  public void setDiffHunks(List<DiffHunk> diffHunks) {
    this.diffHunks = diffHunks;
  }

  /**
   * Clone the object for json serialization
   *
   * @return
   */
  public DiffFile shallowClone() {
    DiffFile diffFile =
        new DiffFile(
            repoID,
            repoName,
            fileID,
            index,
            status,
            fileType,
            baseRelativePath,
            currentRelativePath,
            "",
            "",
            diffHunksMap);
    diffFile.setRawHeaders(this.rawHeaders);
    return diffFile;
  }
}
