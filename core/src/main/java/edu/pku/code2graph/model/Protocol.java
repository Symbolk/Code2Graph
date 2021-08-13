package edu.pku.code2graph.model;

public enum Protocol {
  DEF("def"),
  USE("use"),
  UNKNOWN("any");

  private final String label;

  Protocol(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
