package edu.pku.code2graph.diff.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Counter<T> {

  final ConcurrentMap<T, Integer> counts = new ConcurrentHashMap<>();

  public void put(T it) {
    add(it, 1);
  }

  public int get(T it) {
    return counts.get(it);
  }

  public void add(T it, int v) {
    counts.merge(it, v, Integer::sum);
  }

  public List<T> mostCommon(int n) {
    return counts.entrySet().stream()
        // Sort by value.
        .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
        // Top n.
        .limit(n)
        // Keys only.
        .map(e -> e.getKey())
        // As a list.
        .collect(Collectors.toList());
  }
}
