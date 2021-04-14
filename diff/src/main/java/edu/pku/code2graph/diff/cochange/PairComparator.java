package edu.pku.code2graph.diff.cochange;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;

public class PairComparator implements Comparator<Pair<String, Double>> {
  // descending order
  @Override
  public int compare(Pair<String, Double> p1, Pair<String, Double> p2) {
    if (p1.getRight() < p2.getRight()) {
      return 1;
    } else if (p1.getRight() > p2.getRight()) {
      return -1;
    }
    return 0;
  }
}
