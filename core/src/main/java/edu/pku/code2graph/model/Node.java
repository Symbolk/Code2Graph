package edu.pku.code2graph.model;

public abstract class Node {
  private Integer id;
  protected Type type;
  protected String snippet;
  //    protected Range range;

  public Node() {}

  public Node(Integer id, Type type, String snippet) {
    this.id = id;
    this.type = type;
    this.snippet = snippet;
  }

  public Integer getId() {
    return id;
  }

  public Type getType() {
    return type;
  }

  public String getSnippet() {
    return snippet;
  }
}
