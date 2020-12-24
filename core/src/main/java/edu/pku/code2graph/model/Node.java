package edu.pku.code2graph.model;

public abstract class Node {
  protected Type type;
  protected String snippet;

  public Node() {}

  public Node(Type type, String snippet) {
    this.type = type;
    this.snippet = snippet;
  }

  //    protected Range range;
}
