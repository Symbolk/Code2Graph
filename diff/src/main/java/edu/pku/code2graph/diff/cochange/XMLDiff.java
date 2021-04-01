package edu.pku.code2graph.diff.cochange;

public class XMLDiff {
  private String action = "";
  private String parent = "";
  private String name = "";

  public XMLDiff(String action, String parent, String name) {
    this.action = action;
    this.parent = parent;
    this.name = name;
  }

  public XMLDiff(String action, String name) {
    this.action = action;
    this.name = name;
  }

  public String getAction() {
    return action;
  }

  public String getparent() {
    return parent;
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
        + ", parent='"
        + parent
        + '\''
        + ", name='"
        + name
        + '\''
        + '}';
  }
}
