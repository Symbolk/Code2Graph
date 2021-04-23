package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.model.ChangeType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class XMLDiff {
  private ChangeType changeType = ChangeType.UNKNOWN;
  private String file = "";
  private String tag = ""; // for <X ...>, type is X
  private String name = "";
  private Map<String, Double> contextNodes = new LinkedHashMap<>();

  public XMLDiff(ChangeType changeType, String file, String tag, String name) {
    this.changeType = changeType;
    this.file = file;
    this.tag = tag;
    this.name = name;
  }

  public XMLDiff(
      ChangeType changeType,
      String file,
      String tag,
      String name,
      Map<String, Double> contextNodes) {
    this.changeType = changeType;
    this.file = file;
    this.tag = tag;
    this.name = name;
    this.contextNodes = contextNodes;
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

  public String getTag() {
    return tag;
  }

  public Map<String, Double> getContextNodes() {
    return contextNodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XMLDiff xmlDiff = (XMLDiff) o;
    return Objects.equals(tag, xmlDiff.tag) && Objects.equals(name, xmlDiff.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tag, name);
  }
}
