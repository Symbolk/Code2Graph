package edu.pku.code2graph.mining;

public class Candidate {
  public final String left;
  public final String right;

  public Candidate(String left, String right) {
    if (left.compareTo(right) > 0) {
      this.left = left;
      this.right = right;
    } else {
      this.left = right;
      this.right = left;
    }
  }

  @Override
  public String toString() {
    return left + "," + right;
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
