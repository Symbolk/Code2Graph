package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class Linker {
  private final static Logger logger = LoggerFactory.getLogger(Linker.class);

  public final Rule rule;
  public final URITree tree;

  public Linker(Rule rule, URITree tree) {
    this.rule = rule;
    this.tree = tree;
  }

  /**
   * matched links
   */
  public final List<Link> links = new ArrayList<>();

  /**
   * matched captured
   */
  public final Set<Capture> captures = new HashSet<>();

  /**
   * visited def uris
   */
  public final Set<URI> visited = new HashSet<>();

  /**
   * uri pattern cache
   */
  private final Map<Capture, ScanResult> defCache = new HashMap<>();
  private final Map<Capture, ScanResult> useCache = new HashMap<>();

  private static class ScanResult extends HashMap<Capture, Pair<List<URI>, Set<Capture>>> {}

  private ScanResult scan(URIPattern pattern, Map<Capture, ScanResult> cache, Capture variables) {
    // generate uris
    ScanResult results = new ScanResult();
    Map<URI, List<Node>> uris = tree.get(pattern.getLang());
    if (uris == null) return results;

    // check cache
    variables = variables.project(pattern.anchors);
    if (cache.containsKey(variables)) {
      return cache.get(variables);
    }

    // match every uri
    for (URI uri : uris.keySet()) {
      if (visited.contains(uri)) continue;
      Capture capture = pattern.match(uri, variables);
      if (capture == null) continue;
      Capture key = capture.project(rule.shared);
      Pair<List<URI>, Set<Capture>> pair = results
              .computeIfAbsent(key, k -> new ImmutablePair<>(new ArrayList<>(), new HashSet<>()));
      pair.getLeft().add(uri);
      pair.getRight().add(capture);
    }

    cache.put(variables, results);
    return results;
  }

  private String formatUriList(List<URI> list) {
    Stream<String> segments = list.stream().map(uri -> uri.toString());
    return "[ " + String.join(",\n  ", segments.toArray(String[]::new)) + " ]";
  }

  public void link() {
    link(new Capture());
  }

  public void link(Capture variables) {
    // scan for use patterns
    ScanResult useMap = scan(rule.use, useCache, variables);
    if (useMap.size() == 0) return;

    // scan for def patterns
    ScanResult defMap = scan(rule.def, defCache, variables);
    for (Capture capture : defMap.keySet()) {
      // def capture should match use capture
      Pair<List<URI>, Set<Capture>> uses = useMap.get(capture);
      if (uses == null) continue;

      // check ambiguous links
      Pair<List<URI>, Set<Capture>> defs = defMap.get(capture);
      if (defs.getLeft().size() > 1) {
        System.out.println("ambiguous xll found by " + capture.toString());
        System.out.println(formatUriList(defs.getLeft()));
        System.out.println(formatUriList(uses.getLeft()));
      }

      // generate links
      for (URI use : uses.getLeft()) {
        for (URI def : defs.getLeft()) {
          links.add(new Link(def, use, rule));
        }
        visited.add(use);
      }

      // generate results
      for (Capture use : uses.getRight()) {
        for (Capture def : defs.getRight()) {
          Capture result = new Capture();
          result.putAll(variables);
          result.putAll(def);
          result.putAll(use);
          captures.add(result);
        }
      }
    }
  }
}
