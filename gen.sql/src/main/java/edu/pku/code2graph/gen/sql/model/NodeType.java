package edu.pku.code2graph.gen.sql.model;

import edu.pku.code2graph.model.Type;
import static edu.pku.code2graph.model.TypeSet.type;

public class NodeType {
  public static Type Select = type("select");
  public static Type Column = type("column", true);
  public static Type Table = type("table", true);
  public static Type Update = type("update");
  public static Type Insert = type("insert");
  public static Type Replace = type("replace");
  public static Type Merge = type("merge");
  public static Type Alter = type("alter");
  public static Type AlterView = type("alterView");
  public static Type Drop = type("drop");
  public static Type Delete = type("replace");
  public static Type Truncate = type("truncate");
  public static Type CreateIndex = type("create_index");
  public static Type CreateSchema = type("create_schema");
  public static Type CreateTable = type("create_table");
  public static Type CreateView = type("create_view");
  public static Type With = type("with");
  public static Type Function = type("function");
  public static Type Switch = type("switch");
  public static Type When = type("when");
  public static Type Binary = type("binary");
  public static Type Where = type("where");
  public static Type From = type("from");
  public static Type Join = type("join");
  public static Type Having = type("having");
  public static Type Parameter = type("param");
}
