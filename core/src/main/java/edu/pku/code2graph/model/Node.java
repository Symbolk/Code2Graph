package edu.pku.code2graph.model;

import java.io.Serializable;

import static edu.pku.code2graph.model.TypeSet.type;

public abstract class Node implements Serializable {
  private static final long serialVersionUID = -4685691468295743770L;

  private final Integer id;
  protected Type type;
  protected String snippet;
  protected Language language;
  protected Range range;

  public Node(Integer id, Language language) {
    this.id = id;
    this.language = language;
  }

  public Node(Integer id, Language language, Type type, String snippet) {
    this.id = id;
    this.language = language;
    this.type = type;
    this.snippet = snippet;
  }

  public Integer getId() {
    return id;
  }

  public Type getType() {
    return type == null ? type("OTHER") : type;
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

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public Range getRange() {
    return range;
  }

  public void setRange(Range range) {
    this.range = range;
  }

  public abstract int hashSignature();
}
