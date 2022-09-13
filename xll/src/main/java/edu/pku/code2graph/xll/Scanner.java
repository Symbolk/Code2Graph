package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;

import java.util.HashMap;
import java.util.Map;

public class Scanner {
  /**
   * uri pattern cache
   */
  private final Map<Capture, Map<Capture, Map<URI, Capture>>> cache = new HashMap<>();

  private Map<Capture, Map<URI, Capture>> result;
  private Capture variables;

  public final URIPattern pattern;
  public final Linker linker;
  public final boolean isRef;

  public Scanner(URIPattern pattern, Linker linker, boolean isRef) {
    this.pattern = pattern;
    this.linker = linker;
    this.isRef = isRef;
  }

  public void scan(URITree tree, int index, Capture current) {
    if (pattern.getLayerCount() == index) {
      if (tree.uri != null && !(isRef && linker.visited.contains(tree.uri))) {
        Capture key = current
            .project(linker.rule.def.symbols)
            .project(linker.rule.use.symbols);
        result
            .computeIfAbsent(key, k -> new HashMap<>())
            .put(tree.uri, current);
      }
      return;
    }

    for (Layer layer : tree.children.keySet()) {
      Capture capture = pattern.getLayer(index).match(layer, variables);
      Capture next = current.merge(capture);
      if (next == null) continue;
      scan(tree.children.get(layer), index + 1, next);
    }
  }

  public Map<Capture, Map<URI, Capture>> scan(Capture variables) {
    // check cache
    this.variables = variables = variables.project(pattern.anchors);
    if (cache.containsKey(variables)) {
      return cache.get(variables);
    }

    result = new HashMap<>();
    scan(linker.tree, 0, new Capture());
    cache.put(variables, result);
    return result;
  }
}
