package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.LayerBase;
import edu.pku.code2graph.xll.pattern.AttributePattern;

import java.util.*;

public class LayerPattern extends LayerBase {
  private final URIPattern root;
  private final Map<String, AttributePattern> matchers = new HashMap<>();

  public LayerPattern(String identifier, Language language, URIPattern root) {
    super();
    this.root = root;
    put("identifier", identifier);
    put("language", language.toString());
  }

  @Override
  public String put(String key, String value) {
    String result = super.put(key, value);
    matchers.put(key, AttributePattern.create(key, value, root));
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

    // perform pattern matching
    for (String key : matchers.keySet()) {
      String target = layer.get(key);
      if (target == null) return null;

      AttributePattern matcher = matchers.get(key);
      if (!matcher.match(target, variables, result)) return null;
    }

    return result;
  }
}
