package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Scanner {
  private final static Logger logger = LoggerFactory.getLogger(Scanner.class);

  /**
   * uri pattern cache
   */
  private final Map<Capture, Result> cache = new HashMap<>();

  private Result results;
  private Capture variables;

  public final URIPattern pattern;
  public final Linker linker;

  public Scanner(URIPattern pattern, Linker linker) {
    this.pattern = pattern;
    this.linker = linker;
  }

  public static class Result extends HashMap<Capture, Pair<List<URI>, Set<Capture>>> {}

  public void scan(URITree tree, int index, Capture current) {
    if (pattern.getLayerCount() == index) {
      if (tree.uri != null && !linker.visited.contains(tree.uri)) {
        Capture key = current.project(linker.rule.shared);
        Pair<List<URI>, Set<Capture>> pair = results
            .computeIfAbsent(key, k -> new ImmutablePair<>(new ArrayList<>(), new HashSet<>()));
        pair.getLeft().add(tree.uri);
        pair.getRight().add(current);
      }
      return;
    }

    for (Layer layer : tree.children.keySet()) {
      Capture capture = pattern.getLayer(index).match(layer, variables);
      if (capture == null) continue;
      Capture next = current.clone();
      next.merge(capture);
      scan(tree.children.get(layer), index + 1, next);
    }
  }

  public Result scan(Capture variables) {
    // check cache
    this.variables = variables = variables.project(pattern.anchors);
    if (cache.containsKey(variables)) {
      return cache.get(variables);
    }

    results = new Result();
    scan(linker.tree, 0, new Capture());
    cache.put(variables, results);
    return results;
  }
}
