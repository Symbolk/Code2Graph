package edu.pku.code2graph.model;

public class ElementNode extends Node {
  protected String name;
  protected String qualifiedName;

  public ElementNode(
      Integer id, Language language, Type type, String snippet, String name, String qualifiedName) {
    super(id, language, type, snippet);
    this.name = name;
    this.qualifiedName = qualifiedName;
  }

  public String getName() {
    return name;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setQualifiedName(String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((qualifiedName == null) ? 0 : qualifiedName.hashCode());
    return result;
  }
}
