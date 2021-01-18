package edu.pku.code2graph.diff.model;

public enum Version {
  // use a/b to indicate old/new, left/right, base/current versions, just like Git
  A(0, "a"),
  B(1, "b");

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
