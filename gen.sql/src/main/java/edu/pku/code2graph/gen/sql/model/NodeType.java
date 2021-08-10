package edu.pku.code2graph.gen.sql.model;

import edu.pku.code2graph.model.Type;
import static edu.pku.code2graph.model.TypeSet.type;

public class NodeType {
  public static Type PlainSelect = type("plain_select");
  public static Type Column = type("column", true);
  public static Type Table = type("table", true);
  public static Type Update = type("update");
  public static Type Insert = type("insert");
}
