package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.model.ChangeType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XMLDiff {
  private ChangeType changeType = ChangeType.UNKNOWN;
  private String file = "";
  private String type = "";
  private String name = "";
  private List<Pair<String, Double>> contextNodeIDs = new ArrayList<>();

  public XMLDiff(ChangeType changeType, String file, String name) {
    this.changeType = changeType;
    this.file = file;
    this.name = name;
  }

  public XMLDiff(ChangeType changeType, String file, String name, List<Pair<String, Double>> ids) {
    this.changeType = changeType;
    this.file = file;
    this.name = name;
    this.contextNodeIDs = ids;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public String getFile() {
    return file;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public List<Pair<String, Double>> getContextNodeIDs() {
    return contextNodeIDs;
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
