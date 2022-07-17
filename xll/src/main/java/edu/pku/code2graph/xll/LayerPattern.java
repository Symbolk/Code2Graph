package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.LayerBase;
import edu.pku.code2graph.xll.pattern.AttributePattern;

import java.util.Set;
import java.util.TreeSet;

public class LayerPattern extends LayerBase {
  private final URIPattern root;
  private final Set<AttributePattern> matchers = new TreeSet<>();

  public LayerPattern(String identifier, Language language, URIPattern root) {
    super();
    this.root = root;
    put("identifier", identifier);
    put("language", language.toString());
  }

  public LayerPattern(Layer layer, URIPattern root) {
    this.root = root;
    for (String key : layer.keySet()) {
      put(key, layer.get(key));
    }
  }

  @Override
  public String put(String key, String value) {
    String result = super.put(key, value);
    matchers.add(AttributePattern.create(key, value, root));
    return result;
  }

  /**
   * match layer, return null if not matched, or a capture as result
   * @param layer input layer
   * @param variables context variables
   * @return captures
   */
  public Capture match(Layer layer, Capture variables) {
    Capture result = new Capture();
    if (match(layer, variables, result)) {
      return result;
    } else {
      return null;
    }
  }

  /**
   * match layer, return null if not matched, or a capture as result
   * @param layer input layer
   * @param variables context variables
   * @param result result capture
   * @return captures
   */
  public boolean match(Layer layer, Capture variables, Capture result) {
    // perform pattern matching
    for (AttributePattern matcher : matchers) {
      String target = layer.get(matcher.key);
      if (target == null) return false;
      if (!matcher.match(target, variables, result)) return false;
    }
    return true;
  }

  public Layer refactor(Layer target, Capture input, Capture output) {
    Layer layer = target.clone();
    for (AttributePattern matcher : matchers) {
      String result = matcher.refactor(target.get(matcher.key), input, output);
      if (result == null) return null;
      layer.put(matcher.key, result);
    }
    return layer;
  }
}
