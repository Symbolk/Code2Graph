package edu.pku.code2graph.diff.model;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class DiffHunk {
  private String repoID;
  private String repoName;
  private String fileID;
  private String diffHunkID;
  private String commitID;

  private Integer fileIndex; // the index of the diff file
  private Integer index; // the index of the diff hunk in the current file diff, start from 0
  private Hunk aHunk;
  private Hunk bHunk;
  private FileType fileType;
  private ChangeType changeType;
  private transient List<Action> astActions = new ArrayList<>();
  private transient List<Action> refActions = new ArrayList<>();
  private String description = "";

  // lines from the raw output of git-diff (for patch generation)
  private List<String> rawDiffs = new ArrayList<>();

  public DiffHunk(
      Integer index, FileType fileType, ChangeType changeType, Hunk aHunk, Hunk bHunk) {
    this.index = index;
    this.fileType = fileType;
    this.aHunk = aHunk;
    this.bHunk = bHunk;
    this.changeType = changeType;
  }

  public DiffHunk(
      Integer index,
      FileType fileType,
      ChangeType changeType,
      Hunk aHunk,
      Hunk bHunk,
      String description) {
    this.index = index;
    this.fileType = fileType;
    this.aHunk = aHunk;
    this.bHunk = bHunk;
    this.changeType = changeType;
    this.description = description;
  }

  public Integer getIndex() {
    return index;
  }

  public Hunk getaHunk() {
    return aHunk;
  }

  public Hunk getbHunk() {
    return bHunk;
  }

  public String getRepoID() {
    return repoID;
  }

  public String getFileID() {
    return fileID;
  }

  public void setFileID(String fileID) {
    this.fileID = fileID;
  }

  public String getRepoName() {
    return repoName;
  }

  public String getDiffHunkID() {
    return diffHunkID;
  }

  public void setDiffHunkID(String diffHunkID) {
    this.diffHunkID = diffHunkID;
  }

  public String getCommitID() {
    return commitID;
  }

  public void setCommitID(String commitID) {
    this.commitID = commitID;
  }

  public Integer getAStartLine() {
    return aHunk.getStartLine();
  }

  public Integer getAEndLine() {
    return aHunk.getEndLine();
  }

  public Integer getBStartLine() {
    return bHunk.getStartLine();
  }

  public Integer getBEndLine() {
    return bHunk.getEndLine();
  }

  public Pair<Integer, Integer> getCodeRangeOf(Version version) {
    if (version.equals(Version.A)) {
      return Pair.of(getAStartLine(), getAEndLine());
    } else if (version.equals(Version.B)) {
      return Pair.of(getBStartLine(), getBEndLine());
    }
    return Pair.of(-1, -1);
  }

  public Integer getFileIndex() {
    return fileIndex;
  }

  public void setFileIndex(Integer fileIndex) {
    this.fileIndex = fileIndex;
  }

  public String getUniqueIndex() {
    return fileIndex + ":" + index;
  }

  public FileType getFileType() {
    return fileType;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public List<String> getRawDiffs() {
    return rawDiffs;
  }

  public void setRawDiffs(List<String> rawDiffs) {
    this.rawDiffs = rawDiffs;
  }

  public List<Action> getAstActions() {
    return astActions;
  }

  public List<Action> getRefActions() {
    return refActions;
  }

  public void addASTAction(Action action) {
    if (!astActions.contains(action)) {
      astActions.add(action);
    }
  }

  public void setAstActions(List<Action> astActions) {
    this.astActions = astActions;
  }

  public void addRefAction(Action action) {
    if (!refActions.contains(action)) {
      refActions.add(action);
    }
  }

  public void setRefActions(List<Action> refActions) {
    this.refActions = refActions;
  }

  public String getUUID() {
    return fileID + ":" + diffHunkID;
  }

  public boolean containsCode() {
    return aHunk.getContentType().equals(ContentType.CODE)
        || bHunk.getContentType().equals(ContentType.CODE);
  }

  /**
   * Generate a string description from the actions
   *
   * @return
   */
  public void generateDescription() {
    StringBuilder builder = new StringBuilder();
    for (Action action : astActions) {
      builder.append(action.toString()).append(System.lineSeparator());
    }
    for (Action action : refActions) {
      builder.append(action.toString()).append(System.lineSeparator());
    }
    description = builder.toString();
  }

  public String getDescription() {
    if (description.isEmpty()) {
      generateDescription();
    }
    return description;
  }
}
