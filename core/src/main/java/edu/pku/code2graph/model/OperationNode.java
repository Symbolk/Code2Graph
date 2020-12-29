package edu.pku.code2graph.model;

public class OperationNode extends Node {
  public String symbol;
  public Integer arity;

  public OperationNode(Integer id, Type type, String snippet) {
    super(id, type, snippet);
  }

  public OperationNode(Integer id, Type type, String snippet, String symbol, Integer arity) {
    super(id, type, snippet);
    this.symbol = symbol;
    this.arity = arity;
  }
}
