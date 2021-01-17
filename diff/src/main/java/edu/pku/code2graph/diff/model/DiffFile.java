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
  private String aRelativePath;
  private String bRelativePath;
  private String aContent;
  private String bContent;
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
      String aRelativePath,
      String bRelativePath,
      String aContent,
      String bContent) {
    this.index = index;
    this.status = status;
    this.fileType = fileType;
    this.charset = charset;
    this.aRelativePath = aRelativePath;
    this.bRelativePath = bRelativePath;
    this.aContent = aContent;
    this.bContent = bContent;
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
      String aRelativePath,
      String bRelativePath,
      String aContent,
      String bContent,
      Map<String, DiffHunk> diffHunksMap) {
    this.repoID = repoID;
    this.repoName = repoName;
    this.fileID = fileID;
    this.index = index;
    this.status = status;
    this.description = status.label;
    this.fileType = fileType;
    this.aRelativePath = aRelativePath;
    this.bRelativePath = bRelativePath;
    this.aContent = aContent;
    this.bContent = bContent;
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

  public String getaRelativePath() {
    return aRelativePath;
  }

  public String getbRelativePath() {
    return bRelativePath;
  }

  public String getRelativePathOf(Version version) {
    if (version.equals(Version.A)) {
      return getaRelativePath();
    } else if (version.equals(Version.B)) {
      return getbRelativePath();
    }
    return "";
  }

  public String getaContent() {
    return aContent;
  }

  public String getbContent() {
    return bContent;
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
                aRelativePath,
                bRelativePath,
            "",
            "",
            diffHunksMap);
    diffFile.setRawHeaders(this.rawHeaders);
    return diffFile;
  }
}
