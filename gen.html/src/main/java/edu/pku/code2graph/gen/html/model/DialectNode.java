package edu.pku.code2graph.gen.html.model;

import edu.pku.code2graph.model.Type;

import java.util.ArrayList;
import java.util.List;

public class DialectNode {
  private String name;
  private String snippet;
  private Type type;
  private Integer startIdx;
  private Integer endIdx;
  private DialectNode parent;
  private List<DialectNode> children = new ArrayList<>();

  public DialectNode(String name, String snippet, Type type) {
    this.name = name;
    this.snippet = snippet;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSnippet() {
    return snippet;
  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Integer getStartIdx() {
    return startIdx;
  }

  public void setStartIdx(Integer startIdx) {
    this.startIdx = startIdx;
  }

  public Integer getEndIdx() {
    return endIdx;
  }

  public void setEndIdx(Integer endIdx) {
    this.endIdx = endIdx;
  }

  public DialectNode getParent() {
    return parent;
  }

  public void setParent(DialectNode parent) {
    this.parent = parent;
  }

  public List<DialectNode> getChildren() {
    return children;
  }

  public void setChildren(List<DialectNode> children) {
    this.children = children;
  }
}
