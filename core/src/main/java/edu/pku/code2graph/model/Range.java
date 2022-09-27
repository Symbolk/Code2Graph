package edu.pku.code2graph.model;

import java.io.Serializable;

public class Range implements Serializable {
  private static final long serialVersionUID = -5826525705626590558L;
  private String fileName = null;
  // necessary
  private int startLine = -1;
  private int endLine = -1;
  // optional
  private int startColumn = -1;
  private int endColumn = -1;

  public Range(int startLine, int endLine) {
    this.startLine = startLine;
    this.endLine = endLine;
  }

  public Range(int startLine, int endLine, int startColumn, int endColumn) {
    this.startLine = startLine;
    this.endLine = endLine;
    this.startColumn = startColumn;
    this.endColumn = endColumn;
  }

  public Range(String source) {
    if (source.length() == 0) return;
    String[] startEnd = source.split("~");
    String[] start = startEnd[0].split(":");
    String[] end = startEnd[1].split(":");
    this.startLine = Integer.parseInt(start[0]);
    this.startColumn = Integer.parseInt(start[1]);
    this.endLine = Integer.parseInt(end[0]);
    this.endColumn = Integer.parseInt(end[1]);
  }

  public Range(String source, String fileName) {
    this(source);
    this.fileName = fileName;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public void setStartColumn(int startColumn) {
    this.startColumn = startColumn;
  }

  public int getEndColumn() {
    return endColumn;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setEndColumn(int endColumn) {
    this.endColumn = endColumn;
  }

  public void setStartPosition(int startLine, int startColumn) {
    this.startLine = startLine;
    this.startColumn = startColumn;
  }

  public boolean overlapsWith(Range range) {
    // overlapping intervals: !(b1 < a2 || b2 < a1) = (b1 >= a2 && b2 >= a1)
    // [a1, b1] this
    // [a2, b2] range
    if (range == null) {
      return false;
    }
    if (this.fileName != null && range.fileName != null && !this.fileName.equals(range.fileName)) {
      return false;
    }
    return this.endLine >= range.getStartLine() && range.getEndLine() >= this.startLine;
  }

  public boolean coversInSameLine(Range range) {
    if (range == null) {
      return false;
    }
    if (this.fileName != null && range.fileName != null && !this.fileName.equals(range.fileName)) {
      return false;
    }
    return this.endLine == range.getEndLine()
            && this.startLine == range.startLine
            && this.endColumn >= range.endColumn
            && this.startColumn <= range.startColumn;
  }

  public boolean isValid() {
    return this.startLine >= 0 && this.endLine >= 0;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(startLine).append(":").append(startColumn);
    builder.append("~").append(endLine).append(":").append(endColumn);
    return builder.toString();
  }
}
