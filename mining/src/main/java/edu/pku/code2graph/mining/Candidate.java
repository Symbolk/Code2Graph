package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Set;

public class Candidate {
  public final String source1;
  public final String source2;
  public final String pattern1;
  public final String pattern2;
  public final double similarity;

  public Candidate(Change change1, Change change2, double similarity) {
    this.source1 = change1.source;
    this.source2 = change2.source;
    this.pattern1 = pattern(change1.uri);
    this.pattern2 = pattern(change2.uri);
    this.similarity = similarity;
  }

  @Override
  public String toString() {
    return source1 + "," + source2;
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

  static public String pattern(URI uri) {
    return "";
  }
}
