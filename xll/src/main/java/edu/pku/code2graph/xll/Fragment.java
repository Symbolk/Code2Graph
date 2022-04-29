package edu.pku.code2graph.xll;

import java.util.LinkedList;
import java.util.List;

public class Fragment {
  public final boolean greedy;
  public String text;
  public final String plain;
  public final String format;
  private boolean isAuto;
  private LinkedList<String> slices;

  public Fragment(String text, String format) {
    this.text = text;
    this.plain = text.toLowerCase().replaceAll("[^0-9a-z]", "");
    if (format.equals("auto")) {
      if (text.contains("/")) {
        format = "slash";
      } else if (text.contains("-")) {
        format = "param";
      } else if (text.contains("_")) {
        format = "snake";
      } else if (text.contains(".")) {
        format = "dot";
      } else {
        format = "camel";
      }
    }
    this.format = format;
    this.greedy = format.equals("slash");
  }

  public List<String> slice() {
    if (slices != null) return slices;
    Modifier modifier = Modifier.from(format);
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

  public void align(Fragment old, Fragment contra) {
    slice();
    int offset = contra.slice().size() - old.slice().size();
    if (offset > 0) {
      while (offset-- > 0) {
        slices.addFirst(contra.slice().get(offset));
      }
    } else {
      while (offset++ < 0) {
        slices.removeFirst();
      }
    }
    text = toString(format);
  }

  public String toString(String format) {
    List<String> source = slice();
    if (source == null) return text;
    Modifier modifier = Modifier.from(format);
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
