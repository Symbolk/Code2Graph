package edu.pku.code2graph.model;

public class RelationNode extends Node {
  private static final long serialVersionUID = -2214180402155239327L;

  public String symbol;
  public Integer arity;

  public RelationNode(Integer id, Language language) {
    super(id, language);
  }

  public RelationNode(Integer id, Language language, Type type, String snippet) {
    super(id, language, type, snippet);
  }

  // symbol: +/-/*// of expressions, or symbol for viz
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

  @Override
  public int hashSignature() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((language == null) ? 0 : language.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((snippet == null) ? 0 : snippet.hashCode());
    return result;
  }
}
