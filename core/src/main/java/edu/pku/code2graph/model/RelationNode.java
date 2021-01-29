package edu.pku.code2graph.model;

public class RelationNode extends Node {
  public String symbol;
  public Integer arity;

  public RelationNode(Integer id, Language language) {
    super(id, language);
  }

  public RelationNode(Integer id, Language language, Type type, String snippet) {
    super(id, language, type, snippet);
  }

  public RelationNode(Integer id, Language language, Type type, String snippet, String symbol) {
    super(id, language, type, snippet);
    this.symbol = symbol;
    this.arity = 2;
  }

  public String getSymbol() {
    return symbol == null ? "" : symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Integer getArity() {
    return arity;
  }

  public void setArity(Integer arity) {
    this.arity = arity;
  }
}
