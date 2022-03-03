package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Layer;

import java.util.*;

public class LayerPattern extends Layer {
  public static Set<String> patternAttributes = new HashSet<>();

  static {
    patternAttributes.add("varType");
    patternAttributes.add("identifier");
  }

  private final Map<String, IdentifierPattern> matchers = new HashMap<>();

  public final List<String> anchors = new ArrayList<>();
  public final List<String> symbols = new ArrayList<>();

  public LayerPattern(String identifier, Language language) {
    super(identifier, language);
    IdentifierPattern matcher = new IdentifierPattern(identifier);
    matchers.put("identifier", matcher);
    anchors.addAll(matcher.anchors);
    for (Token token : matcher.symbols) {
      symbols.add(token.name);
    }
  }

  @Override
  public String put(String key, String value) {
    String result = super.put(key, value);
    if (matchers == null) return result;
    if (patternAttributes.contains(key)) {
      IdentifierPattern matcher = new IdentifierPattern(value);
      matchers.put(key, matcher);
      anchors.addAll(matcher.anchors);
      for (Token token : matcher.symbols) {
        symbols.add(token.name);
      }
    }
    return result;
  }

  public Capture match(Layer layer, Capture variables) {
    Capture result = new Capture();

    // perform strict matching
    for (String key : keySet()) {
      String target = layer.getAttribute(key);
      if (target == null) return null;

      if (!patternAttributes.contains(key)) {
        String source = getAttribute(key);
        if (!source.equals(target)) return null;
      }
    }

    // perform pattern matching
    for (String key : matchers.keySet()) {
      IdentifierPattern matcher = matchers.get(key);
      String target = layer.get(key);
      Capture capture = matcher.match(target, variables);
      if (capture == null) return null;
      result.merge(capture);
    }

    return result;
  }
}
