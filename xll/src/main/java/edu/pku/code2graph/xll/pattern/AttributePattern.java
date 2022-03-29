package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;
import edu.pku.code2graph.xll.URIPattern;

import java.util.HashMap;
import java.util.Map;

public abstract class AttributePattern {
  protected final URIPattern root;

  public AttributePattern(final URIPattern root) {
    this.root = root;
  }

  public abstract Capture match(String target, Capture variables);

  static Map<String, Class<? extends AttributePattern>> registry = new HashMap<>();

  static {
    register("language", LanguagePattern.class);
    register("identifier", IdentifierPattern.class);
    register("varType", IdentifierPattern.class);
    register("queryId", IdentifierPattern.class);
    register("resultType", IdentifierPattern.class);
  }

  public static void register(String key, Class<? extends AttributePattern> value) {
    registry.put(key, value);
  }

  public static AttributePattern create(String key, String value, URIPattern root) {
    try {
      return registry.get(key)
          .getDeclaredConstructor(String.class, URIPattern.class)
          .newInstance(value, root);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
