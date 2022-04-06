package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;
import edu.pku.code2graph.xll.URIPattern;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public abstract class AttributePattern implements Comparable<AttributePattern> {
  protected final URIPattern root;
  protected final int order;
  public final String key;

  public AttributePattern(String key, URIPattern root, int order) {
    this.key = key;
    this.root = root;
    this.order = order;
  }

  public int compareTo(AttributePattern target) {
    int result = Integer.compare(order, target.order);
    if (result != 0) return result;
    return key.compareTo(target.key);
  }

  /**
   * match layer attribute
   * @param target input value
   * @param variables context variables
   * @return result capture
   */
  public Capture match(String target, Capture variables) {
    Capture result = new Capture();
    if (match(target, variables, result)) {
      return result;
    } else {
      return null;
    }
  }

  /**
   * match layer attribute
   * @param target input value
   * @param variables context variables
   * @param result result capture
   * @return boolean
   */
  public abstract boolean match(String target, Capture variables, Capture result);

  static final Map<String, Constructor<? extends AttributePattern>> registry = new HashMap<>();

  static {
    try {
      register("language", LanguagePattern.class);
      register("identifier", IdentifierPattern.class);
      register("varType", IdentifierPattern.class);
      register("queryId", IdentifierPattern.class);
      register("resultType", IdentifierPattern.class);
      register("parameterType", IdentifierPattern.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static public void register(String key, Class<? extends AttributePattern> value) throws NoSuchMethodException {
    registry.put(key, value.getDeclaredConstructor(String.class, String.class, URIPattern.class));
  }

  static final Map<String, Exception> exceptions = new HashMap<>();

  static public AttributePattern create(String key, String value, URIPattern root) {
    if (exceptions.containsKey(key)) return null;
    try {
      return registry.get(key).newInstance(key, value, root);
    } catch (Exception e) {
      exceptions.put(key, e);
      e.printStackTrace();
      return null;
    }
  }
}
