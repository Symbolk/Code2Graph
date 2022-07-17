package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;

import java.util.HashMap;
import java.util.Set;

public class Baseline {
  double total = 0;
  double negative = 0;
  public URITree tree = new URITree();
  private HashMap<URI, HashMap<URI, Double>> graph = new HashMap<>();

  public void connect(URI a, URI b, double confidence) {
    HashMap<URI, Double> map1 = graph.computeIfAbsent(a, k -> new HashMap<>());
    HashMap<URI, Double> map2 = graph.computeIfAbsent(b, k -> new HashMap<>());
    double value = Confidence.add(map1.getOrDefault(b, 0.), confidence);
    total = Confidence.add(total, confidence);
    if (confidence == 0) negative++;
    map1.put(b, value);
    map2.put(a, value);
  }

  public void normalize() {
    for (URI left : graph.keySet()) {
      for (URI right : graph.keySet()) {
        double value = graph.get(left).get(right);
        if (value == 0) graph.get(left).put(right, total / negative);
      }
    }
  }
}
