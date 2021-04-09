package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.model.ChangeType;

import java.util.Objects;

public class XMLDiff {
  private ChangeType changeType = ChangeType.UNKNOWN;
  private String parent = "";
  private String name = "";

  public XMLDiff(ChangeType changeType, String parent, String name) {
    this.changeType = changeType;
    this.parent = parent;
    this.name = name;
  }

  public XMLDiff(ChangeType changeType, String name) {
    this.changeType = changeType;
    this.name = name;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public String getparent() {
    return parent;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XMLDiff xmlDiff = (XMLDiff) o;
    return Objects.equals(parent, xmlDiff.parent) && Objects.equals(name, xmlDiff.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent, name);
  }
}
