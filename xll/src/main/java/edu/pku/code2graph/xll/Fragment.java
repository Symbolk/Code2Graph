package edu.pku.code2graph.xll;

import java.util.*;

public class Fragment {
  private static final Map<String, String> separators = new HashMap<>();

  static {
    separators.put("dot", "\\.");
    separators.put("snake", "_");
    separators.put("param", "-");
    separators.put("slash", "/");
    separators.put("camel", "(?=[A-Z])");
    separators.put("pascal", "(?=[A-Z])");
  }

  public final boolean greedy;
  public final String text;
  public final String modifier;
  private List<String> slices;
  private String simplified;

  public Fragment(String text, String modifier) {
    this.text = text;
    this.modifier = modifier;
    this.greedy = modifier.equals("slash");
  }

  public List<String> slice() {
    if (slices != null) return slices;
    String separator = separators.get(modifier);
    if (separator == null) return null;

    slices = new ArrayList<>();
    String[] words = text.split(separator);
    for (int i = words.length - 1; i >= 0; i--) {
      slices.add(words[i].toLowerCase());
    }
    return slices;
  }

  public String simplify() {
    if (simplified != null) return simplified;

    return simplified = text.toLowerCase().replaceAll("[^0-9a-z]", "");
  }

  @Override
  public String toString() {
    return modifier + '=' + text;
  }

  @Override
  public boolean equals(Object o) {
    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public boolean match(Fragment fragment) {
    List<String> source = slice();
    List<String> target = fragment.slice();
    if (source == null || target == null) {
      return simplify().equals(fragment.simplify());
    }

    int sourceSize = source.size();
    int targetSize = target.size();
    if (!greedy && sourceSize > targetSize || sourceSize < targetSize) {
      return false;
    }

    for (int i = 0; i < targetSize; i++) {
      if (!target.get(i).equals(source.get(i))) {
        return false;
      }
    }
    return true;
  }
}
