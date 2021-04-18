package edu.pku.code2graph.diff.cochange;

public enum EntityType {
  FILE("F", "file"),
  TYPE("T", "type"),
  MEMBER("M", "member");

  public String symbol;
  public String label;

  EntityType(String symbol, String label) {
    this.symbol = symbol;
    this.label = label;
  }
}
