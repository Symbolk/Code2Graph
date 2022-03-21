package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.xll.pattern.AttributePattern;
import edu.pku.code2graph.xll.pattern.IdentifierPattern;
import edu.pku.code2graph.xll.pattern.LanguagePattern;

import java.util.*;

public class LayerPattern extends Layer {
  private final Map<String, AttributePattern> matchers = new HashMap<>();

  public final List<String> anchors = new ArrayList<>();
  public final List<String> symbols = new ArrayList<>();

  public LayerPattern(String identifier, Language language) {
    super(identifier, language);
    IdentifierPattern matcher = new IdentifierPattern(identifier);
    matchers.put("identifier", matcher);
    matchers.put("language", new LanguagePattern(language.toString()));
    anchors.addAll(matcher.anchors);
    for (Token token : matcher.symbols) {
      symbols.add(token.name);
    }
  }

  @Override
  public String put(String key, String value) {
    String result = super.put(key, value);
    if (matchers == null) return result;
    if (key.equals("language")) {
      LanguagePattern matcher = new LanguagePattern(value);
      matchers.put(key, matcher);
    } else {
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
