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
  public final String plain;
  public final String format;
  private List<String> slices;

  public Fragment(String text, String format) {
    this.text = text;
    this.plain = text.toLowerCase().replaceAll("[^0-9a-z]", "");
    this.format = format;
    this.greedy = format.equals("slash");
  }

  public List<String> slice() {
    if (slices != null) return slices;
    String separator = separators.get(format);
    if (separator == null) return null;

    slices = new ArrayList<>();
    String[] words = text.split(separator);
    for (int i = words.length - 1; i >= 0; i--) {
      slices.add(words[i].toLowerCase());
    }
    return slices;
  }

  public boolean match(Fragment fragment) {
    List<String> source = slice();
    List<String> target = fragment.slice();
    if (source == null || target == null) {
      if (!greedy) return plain.equals(fragment.plain);
      return plain.endsWith(fragment.plain);
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

  static String capitalize(String word) {
    return (char) (word.charAt(0) - 32) + word.substring(1);
  }

  public String toString(String format) {
    List<String> source = slice();
    if (source == null) return text;
    String separator = separators.get(format);
    if (separator == null) return text;

    StringBuilder builder = new StringBuilder();
    boolean upper = format.equals("camel") || format.equals("pascal");
    boolean initial = format.equals("camel");
    for (int index = 0; index < source.size(); index++) {
      String word = source.get(source.size() - 1 - index);
      if (upper) {
        if (index > 0 || !initial) {
          builder.append(capitalize(word));
        } else {
          builder.append(word);
        }
      } else {
        if (index > 0) builder.append(separator);
        builder.append(word);
      }
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    return format + '=' + text;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Fragment)) return false;
    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
