package edu.pku.code2graph.gen.xml.cochange;

public class XMLDiff {
  private String action = "";
  private String location = "";
  private String schema = "";
  private String name = "";

  public XMLDiff(String action, String location, String schema, String name) {
    this.action = action;
    this.location = location;
    this.schema = schema;
    this.name = name;
  }

  public XMLDiff(String action, String location, String schema) {
    this.action = action;
    this.location = location;
    this.schema = schema;
  }

  public XMLDiff(String action, String location) {
    this.action = action;
    this.location = location;
  }

  public String getAction() {
    return action;
  }

  public String getLocation() {
    return location;
  }

  public String getSchema() {
    return schema;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "XMLDiff{"
        + "action='"
        + action
        + '\''
        + ", location='"
        + location
        + '\''
        + ", schema='"
        + schema
        + '\''
        + ", name='"
        + name
        + '\''
        + '}';
  }
}
