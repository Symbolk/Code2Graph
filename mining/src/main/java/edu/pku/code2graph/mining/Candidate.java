package edu.pku.code2graph.mining;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Set;

public class Candidate {
  public final String left;
  public final String right;
  public final double similarity;

  public Candidate(Change left, Change right) {
    this.left = left.source;
    this.right = right.source;
    this.similarity = similarity(this.left, this.right);
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

  static public Double similarity(String s1, String s2) {
    Pair<Set<String>, Integer> slices1 = slices(s1);
    Pair<Set<String>, Integer> slices2 = slices(s2);
    int length = 0;
    for (String word : slices1.getLeft()) {
      if (!slices2.getLeft().contains(word)) continue;
      length += word.length();
    }
    if (length == 0) return 0.;
    return 2. * length / (slices1.getRight() + slices2.getRight());
  }

  static public Pair<Set<String>, Integer> slices(String input) {
    Set<String> result = new HashSet<>();
    int length = 0;
    for (String word : input.split("[^0-9a-zA-Z]|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])|(?=[0-9]([a-z]|[A-Z]{2}))")) {
      if (word.length() > 1) {
        result.add(word.toLowerCase());
        length += word.length();
      }
    }
    return new ImmutablePair<>(result, length);
  }
}
