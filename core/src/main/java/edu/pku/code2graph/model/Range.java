package edu.pku.code2graph.model;

import java.io.Serializable;

public class Range implements Serializable {
  private static final long serialVersionUID = -5826525705626590558L;
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

  public void setEndColumn(int endColumn) {
    this.endColumn = endColumn;
  }

  public boolean overlapsWith(Range range) {
    // overlapping intervals: !(b1 < a2 || b2 < a1) = (b1 >= a2 && b2 >= a1)
    // [a1, b1] this
    // [a2, b2] range
    if (range == null) {
      return false;
    }
    return this.endLine >= range.getStartLine() && range.getEndLine() >= this.startLine;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(startLine).append(":").append(startColumn);
    builder.append("~").append(endLine).append(":").append(endColumn);
    return builder.toString();
  }
}
