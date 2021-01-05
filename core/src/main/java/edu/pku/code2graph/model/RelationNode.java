package edu.pku.code2graph.model;

public class RelationNode extends Node {
  public String symbol;
  public Integer arity;

  public RelationNode(Integer id) {
    super(id);
  }

  public RelationNode(Integer id, Type type, String snippet) {
    super(id, type, snippet);
  }

  public RelationNode(Integer id, Type type, String snippet, String symbol, Integer arity) {
    super(id, type, snippet);
    this.symbol = symbol;
    this.arity = arity;
  }
}
