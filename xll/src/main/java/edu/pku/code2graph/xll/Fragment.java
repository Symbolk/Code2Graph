package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.List;

public class Fragment {
  public final String text;
  public final String modifier;
  public final List<String> slices;

  public Fragment(String text, String modifier) {
    this.text = text;
    this.modifier = modifier;
    if (modifier.equals("dot")) {
      slices = new ArrayList<>();
      for (String word : text.split("\\.")) {
        slices.add(word.toLowerCase());
      }
    } else {
      slices = null;
    }
  }

  @Override
  public String toString() {
    return text + '=' + modifier;
  }

  @Override
  public boolean equals(Object o) {
    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public static boolean match() {
    return true;
  }
}
