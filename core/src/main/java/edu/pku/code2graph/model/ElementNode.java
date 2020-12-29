package edu.pku.code2graph.model;

public class ElementNode extends Node {
  protected String name;
  protected String qualifiedName;

  public ElementNode(Integer id, Type type, String snippet, String name, String qualifiedName) {
    super(id, type, snippet);
    this.name = name;
    this.qualifiedName = qualifiedName;
  }
}
