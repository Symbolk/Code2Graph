package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class NodeType {
  // Elements
  public static final Type CLASS_DECLARATION = type("class_declaration");
  public static final Type INTERFACE_DECLARATION = type("interface_declaration");
  public static final Type ENUM_DECLARATION = type("enum_declaration");

  public static final Type INIT_BLOCK_DECLARATION = type("init_block_declaration");
  public static final Type FIELD_DECLARATION = type("field_declaration");
  public static final Type METHOD_DECLARATION = type("method_declaration");
  public static final Type VAR_DECLARATION = type("variable_declaration");

  // Relations
  // block
  public static final Type BLOCK = type("block");

  // expression
  public static final Type ASSIGNMENT_OPERATOR = type("assign");
  public static final Type INFIX_OPERATOR = type("infix");
  public static final Type PREFIX_OPERATOR = type("prefix");
  public static final Type POSTFIX_OPERATOR = type("posfix");

  public static final Type FIELD_ACCESS = type("field_access");
  public static final Type METHOD_INVOCATION = type("method_invocation");
  public static final Type PARAMETER_ACCESS = type("parameter_access");
  public static final Type TYPE_INSTANTIATION = type("type_instantiation");

  // control relations
  public static final Type IF_STATEMENT = type("if_statement");
  public static final Type FOR_STATEMENT = type("for_statement");
  public static final Type WHILE_STATEMENT = type("while_statement");
  public static final Type SWITCH_STATEMENT = type("switch_statement");
  public static final Type TRY_STATEMENT = type("try_statement");
}
