package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.xll.Link;
import edu.pku.code2graph.xll.Linker;
import edu.pku.code2graph.xll.Rule;
import edu.pku.code2graph.xll.URIPattern;

import java.util.ArrayList;
import java.util.HashMap;

public class Baseline {
  double sum = 0;
  double negative = 0;
  public URITree tree = new URITree();
  public ArrayList<Rule> rules = new ArrayList<>();
  private HashMap<URI, HashMap<URI, Double>> graph = new HashMap<>();

  public void connect(URI a, URI b, double confidence) {
    HashMap<URI, Double> map1 = graph.computeIfAbsent(a, k -> new HashMap<>());
    HashMap<URI, Double> map2 = graph.computeIfAbsent(b, k -> new HashMap<>());
    double value = Confidence.add(map1.getOrDefault(b, 0.), confidence);
    sum = Confidence.add(sum, confidence);
    if (confidence == 0) negative++;
    map1.put(b, value);
    map2.put(a, value);
  }

  public void normalize() {
    for (URI left : graph.keySet()) {
      for (URI right : graph.keySet()) {
        HashMap<URI, Double> map = graph.get(left);
        double value = map.get(right);
        if (value == 0) {
          map.put(right, -sum / negative);
        }
      }
    }
  }

  public double evaluate(URIPattern def, URIPattern use) {
    Linker linker = new Linker(tree, def, use);
    linker.link();
    double sum = 0;
    for (Link link : linker.links) {
      URI left = link.def;
      URI right = link.use;
      double value = graph.get(left).get(right);
      sum = Confidence.add(sum, value);
    }
    return sum;
  }
}
