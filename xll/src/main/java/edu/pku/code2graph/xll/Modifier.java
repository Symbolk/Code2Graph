package edu.pku.code2graph.xll;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public abstract class Modifier {
  final String prefix;
  final boolean ignoreFirst;

  protected Modifier(String prefix, boolean ignoreFirst) {
    this.prefix = prefix;
    this.ignoreFirst = ignoreFirst;
  }

  static String capitalize(String word) {
    return (char) (word.charAt(0) - 32) + word.substring(1);
  }

  void append(StringBuilder builder, String word, int index) {
    if (index > 0) builder.append(prefix);
    if (prefix.isEmpty() && (index > 0 || !ignoreFirst)) {
      builder.append(capitalize(word));
    } else {
      builder.append(word);
    }
  }

  abstract LinkedList<String> split(String text);

  static final Map<String, Modifier> registry = new HashMap<>();

  static {
    registry.put("dot", new Modifier.Separator("."));
    registry.put("snake", new Modifier.Separator("_"));
    registry.put("param", new Modifier.Separator("-"));
    registry.put("slash", new Modifier.Separator("/"));
    registry.put("camel", new Modifier.Capital(true));
    registry.put("pascal", new Modifier.Capital(false));
  }

  public static Modifier from(String format) {
    return registry.get(format);
  }

  public static class Separator extends Modifier {
    final String regex;

    Separator(String prefix) {
      super(prefix, true);
      this.regex = prefix.replace(".", "\\.");
    }

    LinkedList<String> split(String text) {
      return new LinkedList<>(Arrays.asList(text.split(regex)));
    }
  }

  public static class Capital extends Modifier {
    Capital(boolean ignoreFirst) {
      super("", ignoreFirst);
    }

    LinkedList<String> split(String text) {
      LinkedList<String> result = new LinkedList<>();
      for (String word : text.split("(?<![A-Z0-9])(?=[A-Z])|(?=[A-Z][^A-Z0-9])|(?=[0-9]([a-z]|[A-Z]{2}))")) {
        result.add(word.toLowerCase());
      }
      return result;
    }
  }
}
