package edu.pku.code2graph.model;

public class ElementNode extends Node {
  protected String name;
  protected String qualifiedName;

  public ElementNode(Type type, String snippet, String name, String qualifiedName) {
    super(type, snippet);
    this.name = name;
    this.qualifiedName = qualifiedName;
  }
}
