package edu.pku.code2graph.xll;

import java.util.*;

public class Fragment {
  private static final Map<String, Modifier> modifiers = new HashMap<>();

  static {
    modifiers.put("dot", new Modifier.Separator("."));
    modifiers.put("snake", new Modifier.Separator("_"));
    modifiers.put("param", new Modifier.Separator("-"));
    modifiers.put("slash", new Modifier.Separator("/"));
    modifiers.put("camel", new Modifier.Capital(true));
    modifiers.put("pascal", new Modifier.Capital(false));
  }

  public final boolean greedy;
  public String text;
  public final String plain;
  public final String format;
  private LinkedList<String> slices;

  public Fragment(String text, String format) {
    this.text = text;
    this.plain = text.toLowerCase().replaceAll("[^0-9a-z]", "");
    this.format = format;
    this.greedy = format.equals("slash");
  }

  public List<String> slice() {
    if (slices != null) return slices;
    Modifier modifier = modifiers.get(format);
    if (modifier == null) return null;

    return slices = modifier.split(text);
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

    for (int i = 1; i <= targetSize; i++) {
      if (!target.get(targetSize - i).equals(source.get(sourceSize - i))) {
        return false;
      }
    }
    return true;
  }

  public void align(List<String> words) {
    int count = slice().size() - words.size();
    if (count > 0) {
      while (count-- > 0) {
        slices.removeFirst();
      }
    } else {
      while (count++ < 0) {
        slices.addFirst(words.get(-count));
      }
    }
    text = toString(format);
  }

  public String toString(String format) {
    List<String> source = slice();
    if (source == null) return text;
    Modifier modifier = modifiers.get(format);
    if (modifier == null) return text;

    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < source.size(); index++) {
      String word = source.get(index);
      modifier.append(builder, word, index);
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
