package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.xll.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class Analyzer {
  public double sum = 0;
  public int positive = 0;
  public int negative = 0;
  public URITree tree = new URITree();
  public ArrayList<Rule> rules = new ArrayList<>();
  private HashMap<String, HashMap<String, Double>> graph = new HashMap<>();

  public void addAll(Collection<String> uris) {
    // categorized by language
    // pay special attention to the file names
    Map<String, List<Pair<String, String>>> clusters = new HashMap<>();
    for (String source : uris) {
      URI uri = new URI(source);
      Layer last = uri.layers.get(uri.layers.size() - 1);
      Pair<String, String> identifier = Confidence.splitLast(last.get("identifier"), '/');
      if (uri.layers.size() == 1) {
        Pair<String, String> division = Confidence.splitLast(identifier.getRight(), '.');
        String extension = division.getRight().toUpperCase();
        List<Pair<String, String>> cluster = clusters.computeIfAbsent(extension, k -> new ArrayList<>());
        cluster.add(new ImmutablePair<>(division.getLeft(), source));
      } else {
        String language = uri.layers.get(1).get("language");
        List<Pair<String, String>> cluster = clusters.computeIfAbsent(language, k -> new ArrayList<>());
        cluster.add(new ImmutablePair<>(identifier.getRight(), source));
      }
    }

    // build n-partite graph
    String[] languages = clusters.keySet().toArray(new String[clusters.size()]);
    for (int i = 0; i < languages.length; ++i) {
      System.out.println(String.format("  %s: %d", languages[i], clusters.get(languages[i]).size()));
    }
    for (int i = 0; i < languages.length - 1; ++i) {
      List<Pair<String, String>> cluster1 = clusters.get(languages[i]);
      for (int j = i + 1; j < languages.length; ++j) {
        List<Pair<String, String>> cluster2 = clusters.get(languages[j]);
        for (Pair<String, String> entry1 : cluster1) {
          for (Pair<String, String> entry2 : cluster2) {
            if (!Confidence.intersects(entry1.getLeft(), entry2.getLeft())) continue;
//            System.out.println(entry1.getLeft() + " " + entry2.getLeft());
            connect(entry1.getRight(), entry2.getRight(), 1);
          }
        }
      }
    }
  }

  public void connect(String a, String b, double confidence) {
    HashMap<String, Double> map1 = graph.computeIfAbsent(a, k -> new HashMap<>());
    HashMap<String, Double> map2 = graph.computeIfAbsent(b, k -> new HashMap<>());
    double value = Confidence.add(map1.getOrDefault(b, 0.), confidence);
    sum = Confidence.add(sum, confidence);
    positive++;
    if (confidence == 0) negative++;
    map1.put(b, value);
    map2.put(a, value);
  }

  public void normalize() {
    for (String left : graph.keySet()) {
      for (String right : graph.keySet()) {
        HashMap<String, Double> map = graph.get(left);
        double value = map.get(right);
        if (value == 0) {
          map.put(right, -sum / negative);
        }
      }
    }
  }

  public void generalize(URI left, URI right) {
    double value = graph.get(left).get(right);
    if (value <= 0) return;
    boolean flag = false;
    URIPattern patternL = new URIPattern(left);
    URIPattern patternR = new URIPattern(right);
    int countL = left.getLayerCount();
    int countR = right.getLayerCount();
    for (int i = 0; i < countL; ++i) {
      Layer layer = left.getLayer(i);
      for (String key : layer.keySet()) {
        URIPattern newPattern = new URIPattern(left);
        LayerPattern newLayer = new LayerPattern(layer, newPattern);
        newLayer.remove(key);
        newPattern.setLayer(i, newLayer);
        double newValue = evaluate(newPattern, new URIPattern(right));
        if (newValue > value) {
          patternL.getLayer(i).remove(key);
          flag = true;
        }
      }
    }
    for (int i = 0; i < countR; ++i) {
      Layer layer = right.getLayer(i);
      for (String key : layer.keySet()) {
        URIPattern newPattern = new URIPattern(right);
        LayerPattern newLayer = new LayerPattern(layer, newPattern);
        newLayer.remove(key);
        newPattern.setLayer(i, newLayer);
        double newValue = evaluate(new URIPattern(left), newPattern);
        if (newValue > value) {
          patternR.getLayer(i).remove(key);
          flag = true;
        }
      }
    }
    if (flag) {
      rules.add(new Rule(patternL, patternR));
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
