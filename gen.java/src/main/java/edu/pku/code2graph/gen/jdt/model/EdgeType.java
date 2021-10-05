package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class EdgeType {
  // type hierarchy
  public static final Type EXTENDED_CLASS = type("extended_class");
  public static final Type IMPLEMENTED_INTERFACE = type("implemented_interface");
  public static final Type ANNOTATION = type("annotation");
  public static final Type THROWN_EXCEPTION = type("thrown_exception");

  // type and source
  public static final Type DATA_TYPE = type("data_type");
  public static final Type REFERENCE = type("reference");

  // roles for method declaration
  public static final Type PARAMETER = type("parameter");
  public static final Type RETURN_TYPE = type("return_type");

  // roles for method invocation
  public static final Type CALLER = type("caller");
  public static final Type CALLEE = type("callee");
  public static final Type ACCESSOR = type("accessor");
  public static final Type ARGUMENT = type("argument");

  // nesting
  public static final Type BODY = type("body");
  public static final Type CHILD = type("child");

  // operand
  public static final Type LEFT = type("left");
  public static final Type RIGHT = type("right");

  // control
  // if
  public static final Type CONDITION = type("condition");
  public static final Type THEN = type("then");
  public static final Type ELSE = type("else");

  // for
  public static final Type INITIALIZER = type("initializer");
  public static final Type UPDATER = type("updater");

  // enhanced for/for-each
  public static final Type ELEMENT = type("element");
  public static final Type VALUES = type("values");

  // cast expression
  public static final Type TARGET_TYPE = type("target_type");
  public static final Type CASTED_OBJECT = type("casted_object");

  // try-catch
  public static final Type CATCH = type("catch");
  public static final Type FINALLY = type("finally");

  // throw
  public static final Type THROW = type("throw");

  // sql
  public static final Type INLINE_SQL = type("inline_sql_query");
}
