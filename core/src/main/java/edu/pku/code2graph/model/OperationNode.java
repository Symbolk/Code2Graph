package edu.pku.code2graph.model;

public class OperationNode extends Node {
  public String symbol;
  public Integer arity;

  public OperationNode(Type type, String snippet) {
    super(type, snippet);
  }

  public OperationNode(Type type, String snippet, String symbol, Integer arity) {
    super(type, snippet);
    this.symbol = symbol;
    this.arity = arity;
  }
}
