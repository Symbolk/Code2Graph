package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.LayerBase;
import edu.pku.code2graph.xll.Capture;

import java.util.*;

public class LayerPattern extends LayerBase {
  private final URIPattern root;
  private final Map<String, AttributePattern> matchers = new HashMap<>();

  static Map<String, Class<? extends AttributePattern>> attributes = new HashMap<>();

  static {
    attributes.put("language", LanguagePattern.class);
    attributes.put("identifier", IdentifierPattern.class);
    attributes.put("varType", IdentifierPattern.class);
  }

  public LayerPattern(String identifier, Language language, URIPattern root) {
    super();
    this.root = root;
    put("identifier", identifier);
    put("language", language.toString());
  }

  @Override
  public String put(String key, String value) {
    String result = super.put(key, value);
    try {
      AttributePattern matcher = (AttributePattern) attributes.get(key).getDeclaredConstructors()[0].newInstance(value, root);
      matchers.put(key, matcher);
    } catch (Exception e) {
      e.printStackTrace();
    }
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
      if (capture == null) return null;
      result.putAll(capture);
    }

    return result;
  }
}
