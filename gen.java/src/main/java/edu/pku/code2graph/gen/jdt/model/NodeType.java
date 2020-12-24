package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class NodeType {
  // OperationNode
  private static final Type ASSIGNMENT_OPERATOR = type("ASSIGNMENT_OPERATOR");
  private static final Type INFIX_EXPRESSION_OPERATOR = type("INFIX_EXPRESSION_OPERATOR");
  private static final Type PREFIX_EXPRESSION_OPERATOR = type("PREFIX_EXPRESSION_OPERATOR");
  private static final Type POSTFIX_EXPRESSION_OPERATOR = type("POSTFIX_EXPRESSION_OPERATOR");

  private static final Type METHOD_INVOCATION = type("METHOD_INVOCATION");
  private static final Type TYPE_INSTANTIATION = type("TYPE_INSTANTIATION");

  // DeclarationNode
  private static final Type TYPE_DECLARATION = type("TYPE_DECLARATION");
  private static final Type METHOD_DECLARATION = type("METHOD_DECLARATION");

  // BlockNode
  private static final Type BLOCK = type("BLOCK");

  // ControlNode

}
