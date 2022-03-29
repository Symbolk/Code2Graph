package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;
import edu.pku.code2graph.xll.URIPattern;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public abstract class AttributePattern {
  protected final URIPattern root;

  public AttributePattern(final URIPattern root) {
    this.root = root;
  }

  public abstract Capture match(String target, Capture variables);

  static final Map<String, Constructor<? extends AttributePattern>> registry = new HashMap<>();

  static {
    try {
      register("language", LanguagePattern.class);
      register("identifier", IdentifierPattern.class);
      register("varType", IdentifierPattern.class);
      register("queryId", IdentifierPattern.class);
      register("resultType", IdentifierPattern.class);
      register("paramType", IdentifierPattern.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static public void register(String key, Class<? extends AttributePattern> value) throws NoSuchMethodException {
    registry.put(key, value.getDeclaredConstructor(String.class, URIPattern.class));
  }

  static final Map<String, Exception> exceptions = new HashMap<>();

  static public AttributePattern create(String key, String value, URIPattern root) {
    if (exceptions.containsKey(key)) return null;
    try {
      return registry.get(key).newInstance(value, root);
    } catch (Exception e) {
      exceptions.put(key, e);
      e.printStackTrace();
      return null;
    }
  }
}
