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

  public Capture match(Layer layer, Capture variables) {
    Capture result = new Capture();

    // perform pattern matching
    for (String key : matchers.keySet()) {
      String target = layer.get(key);
      if (target == null) return null;

      AttributePattern matcher = matchers.get(key);
      Capture capture = matcher.match(target, variables);
      if (!result.accept(capture)) return null;
    }

    return result;
  }
}
