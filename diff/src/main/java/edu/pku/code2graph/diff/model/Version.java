package edu.pku.code2graph.diff.model;

public enum Version {
  LEFT(0, "left"),
  RIGHT(1, "right");

  private int index;
  private String label;

  Version(int index, String label) {
    this.index = index;
    this.label = label;
  }

  public String asString() {
    return label;
  }
}
