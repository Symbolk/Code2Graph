package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class NodeType {
  // OperationNode
  public static final Type ASSIGNMENT_OPERATOR = type("ASSIGNMENT_OPERATOR");
  public static final Type INFIX_EXPRESSION_OPERATOR = type("INFIX_EXPRESSION_OPERATOR");
  public static final Type PREFIX_EXPRESSION_OPERATOR = type("PREFIX_EXPRESSION_OPERATOR");
  public static final Type POSTFIX_EXPRESSION_OPERATOR = type("POSTFIX_EXPRESSION_OPERATOR");

  public static final Type METHOD_INVOCATION = type("METHOD_INVOCATION");
  public static final Type TYPE_INSTANTIATION = type("TYPE_INSTANTIATION");

  // DeclarationNode
  public static final Type TYPE_DECLARATION = type("TYPE_DECLARATION");
  public static final Type METHOD_DECLARATION = type("METHOD_DECLARATION");

  // BlockNode
  public static final Type BLOCK = type("BLOCK");

  // ControlNode

}
