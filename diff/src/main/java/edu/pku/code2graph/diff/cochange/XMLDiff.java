package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.model.ChangeType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XMLDiff {
  private ChangeType changeType = ChangeType.UNKNOWN;
  private String type = "";
  private String name = "";
  private List<Pair<String, Double>> siblingIDs = new ArrayList<>();

  public XMLDiff(ChangeType changeType, String type, String name) {
    this.changeType = changeType;
    this.type = type;
    this.name = name;
  }

  public XMLDiff(ChangeType changeType, String name) {
    this.changeType = changeType;
    this.name = name;
  }

  public XMLDiff(ChangeType changeType, String name, List<Pair<String, Double>> ids) {
    this.changeType = changeType;
    this.name = name;
    this.siblingIDs = ids;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public List<Pair<String, Double>> getSiblingIDs() {
    return siblingIDs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XMLDiff xmlDiff = (XMLDiff) o;
    return Objects.equals(type, xmlDiff.type) && Objects.equals(name, xmlDiff.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name);
  }
}
