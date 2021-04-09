package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.model.ChangeType;

import java.util.Objects;

public class JavaDiff {
  private ChangeType changeType = ChangeType.UNKNOWN;

  private String file = "";
  private String type = "";
  private String member = "";

  public JavaDiff(ChangeType changeType, String file, String type, String member) {
    this.changeType = changeType;
    this.file = file;
    this.type = type;
    this.member = member;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public String getFile() {
    return file;
  }

  public String getType() {
    return type;
  }

  public String getMember() {
    return member;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JavaDiff javaDiff = (JavaDiff) o;
    return Objects.equals(file, javaDiff.file)
        && Objects.equals(type, javaDiff.type)
        && Objects.equals(member, javaDiff.member);
  }

  @Override
  public int hashCode() {
    return Objects.hash(file, type, member);
  }
}
