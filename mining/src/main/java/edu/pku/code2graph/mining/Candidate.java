package edu.pku.code2graph.mining;

public class Candidate {
  public final String pattern1;
  public final String pattern2;

  public Candidate(String pattern1, String pattern2) {
    this.pattern1 = pattern1;
    this.pattern2 = pattern2;
  }

  @Override
  public String toString() {
    return pattern1 + "," + pattern2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
