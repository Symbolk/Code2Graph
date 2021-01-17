package edu.pku.code2graph.diff.model;

import com.google.common.collect.Iterables;

import java.util.List;

public class Hunk {
  private String relativeFilePath;
  private Integer startLine;
  private Integer endLine;
  private Version version;
  private ContentType contentType;
  private List<String> codeSnippet;

  public Hunk(
      Version version,
      String relativeFilePath,
      Integer startLine,
      Integer endLine,
      ContentType contentType,
      List<String> codeSnippet) {
    this.version = version;
    this.relativeFilePath = relativeFilePath;
    this.startLine = startLine;
    this.endLine = endLine;
    this.contentType = contentType;
    this.codeSnippet = codeSnippet;
  }

  public Version getVersion() {
    return version;
  }

  public String getRelativeFilePath() {
    return relativeFilePath;
  }

  public Integer getStartLine() {
    return startLine;
  }

  public Integer getEndLine() {
    return endLine;
  }

  public List<String> getCodeSnippet() {
    return codeSnippet;
  }

  /**
   * Get the length of the last line in the code snippets
   *
   * @return
   */
  public int getLastLineLength() {
    String lastLineRaw = Iterables.getLast(codeSnippet, null);
    if (lastLineRaw == null) {
      return 0;
    } else {
      return lastLineRaw.length();
    }
  }

  public ContentType getContentType() {
    return contentType;
  }
}
