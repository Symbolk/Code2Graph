package edu.pku.code2graph.model;

import static edu.pku.code2graph.model.TypeSet.type;

public abstract class Node {
  private Integer id;
  protected Type type;
  protected String snippet;
  //    protected Range range;

  public Node(Integer id) {
    this.id = id;
  }

  public Node(Integer id, Type type, String snippet) {
    this.id = id;
    this.type = type;
    this.snippet = snippet;
  }

  public Integer getId() {
    return id;
  }

  public Type getType() {
    if(type == null) {
      return type("OTHER");
    }
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
  }

  public String getSnippet() {
    return snippet;
  }
}
