package edu.pku.code2graph.mining;

import edu.pku.code2graph.cache.HistoryLoader;
import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.xll.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

public class Analyzer {
  public static int MAX_COCHANGE_SIZE = 1000000;

  public double sum = 0;
  public int positive = 0;
  public int negative = 0;
  public int cochanges = 0;
  public URITree tree = new URITree();
  public ArrayList<Rule> rules = new ArrayList<>();
  public Map<Candidate, Credit> graph = new HashMap<>();

  private HistoryLoader history;

  public Analyzer(HistoryLoader history) {
    this.history = history;
  }

  public void analyze(String hash) throws IOException {
    HistoryLoader.Diff diff = history.diff(hash);
    String format;
    format = String.format("[%s] +%d", hash, diff.additions.size());
    collect(diff.additions, hash, format);
    format = String.format("[%s] -%d", hash, diff.deletions.size());
    collect(diff.deletions, hash, format);
  }

  public void analyzeAll() throws IOException {
    for (String hash : history.commits.keySet()) {
      analyze(hash);
    }
  }

  public void collect(Collection<String> uris, String commit, String format) {
    // categorized by language
    // pay special attention to the file names
    Map<String, List<Change>> clusters = new HashMap<>();
    for (String source : uris) {
      URI uri = new URI(source);
      Layer last = uri.layers.get(uri.layers.size() - 1);
      String identifier = Credit.getLastSegment(last.get("identifier"));
      if (uri.layers.size() == 1) {
        Pair<String, String> division = Credit.splitExtension(identifier);
        String extension = division.getRight().toUpperCase();
        List<Change> cluster = clusters.computeIfAbsent(extension, k -> new ArrayList<>());
        cluster.add(new Change(division.getLeft(), source, uri));
      } else {
        String language = uri.layers.get(1).get("language");
        List<Change> cluster = clusters.computeIfAbsent(language, k -> new ArrayList<>());
        cluster.add(new Change(identifier, source, uri));
      }
    }

    // estimate graph size
    if (clusters.isEmpty()) return;
    StringBuilder builder = new StringBuilder(format);
    String[] keys = clusters.keySet().toArray(new String[0]);
    String[] notes = Arrays.stream(keys).map(k -> k + ": " + clusters.get(k).size()).toArray(String[]::new);
    builder.append(" (").append(String.join(", ", notes)).append(") -> ");
    int estimated = 0;
    for (int i = 0; i < keys.length - 1; ++i) {
      int size1 = clusters.get(keys[i]).size();
      for (int j = i + 1; j < keys.length; ++j) {
        int size2 = clusters.get(keys[j]).size();
        estimated += size1 * size2;
      }
    }
    if (estimated > MAX_COCHANGE_SIZE) {
      builder.append("pruned");
      System.out.println(builder);
      return;
    }
    cochanges += estimated;

    // build n-partite graph
    String[] languages = clusters.keySet().toArray(new String[0]);
    Map<String, Integer> degrees = new HashMap<>();
    List<Candidate> entries = new ArrayList<>();
    for (int i = 0; i < languages.length - 1; ++i) {
      List<Change> cluster1 = clusters.get(languages[i]);
      for (int j = i + 1; j < languages.length; ++j) {
        List<Change> cluster2 = clusters.get(languages[j]);
        for (Change entry1 : cluster1) {
          for (Change entry2 : cluster2) {
            Candidate candidate = new Candidate(entry1, entry2);
            if (candidate.similarity <= 0.5) continue;
            String source1 = entry1.source;
            String source2 = entry2.source;
            degrees.put(source1, degrees.computeIfAbsent(source1, k -> 0) + 1);
            degrees.put(source2, degrees.computeIfAbsent(source2, k -> 0) + 1);
            entries.add(candidate);
          }
        }
      }
    }

    // normalize the graph
    System.out.println(builder.append(entries.size()).append(" collected"));
    for (Candidate candidate : entries) {
      int density = degrees.get(candidate.left) * degrees.get(candidate.right);
      Credit.Record record = new Credit.Record(commit, candidate.similarity, density);
      Credit credit = graph.computeIfAbsent(candidate, k -> new Credit());
      credit.add(record);
      sum = Credit.add(sum, record.value);
    }
  }

  public void normalize() {
//    for (String left : graph.keySet()) {
//      for (String right : graph.keySet()) {
//        HashMap<String, Double> map = graph.get(left);
//        double value = map.get(right);
//        if (value == 0) {
//          map.put(right, -sum / negative);
//        }
//      }
//    }
  }

  public void generalize(URI left, URI right) {
    double value = 0; // graph.get(new Candidate(left.toString(), right.toString())).value;
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
      double value = 0; // graph.get(new Candidate(left.toString(), right.toString())).value;
      sum = Credit.add(sum, value);
    }
    return sum;
  }
}
