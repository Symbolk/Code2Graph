package edu.pku.code2graph.diff.model;

/** Change type of a DiffHunk */
public enum ChangeType {
  UPDATED("U", "Update"),
  ADDED("A", "Add"),
  DELETED("D", "Delete"),
  MOVED("M", "Move"),
  UNKNOWN("U", "Unknown");

  public String symbol;
  public String label;

  ChangeType(String symbol, String label) {
    this.symbol = symbol;
    this.label = label;
  }
}
