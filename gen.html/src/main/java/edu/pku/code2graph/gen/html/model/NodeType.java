package edu.pku.code2graph.gen.html.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class NodeType {
  // Dialect
  public static final Type VAR = type("variable");
  public static final Type INLINE_VAR = type("inline_variable");

  // Graph Node/Edge
  public static final Type CHILD = type("child");
  public static final Type ATTR = type("attribute");
  public static final Type INLINE = type("inline");
  public static final Type ELE = type("element");

  // file
  public static final Type FILE = type("file");
}
