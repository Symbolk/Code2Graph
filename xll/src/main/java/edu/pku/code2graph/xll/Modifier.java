package edu.pku.code2graph.xll;

public abstract class Modifier {
  abstract String[] split(String text);
  abstract void append(StringBuilder builder, String word, int index);

  public static class Separator extends Modifier {
    final String text;
    final String regex;

    Separator(String text) {
      this.text = text;
      this.regex = text.replace(".", "\\.");
    }

    String[] split(String text) {
      if (text.isEmpty()) return new String[]{};
      return text.split(regex);
    }

    void append(StringBuilder builder, String word, int index) {
      if (index > 0) builder.append(text);
      builder.append(word);
    }
  }

  public static class Capital extends Modifier {
    final boolean ignoreFirst;

    Capital(boolean ignoreFirst) {
      this.ignoreFirst = ignoreFirst;
    }

    static String capitalize(String word) {
      return (char) (word.charAt(0) - 32) + word.substring(1);
    }

    String[] split(String text) {
      return text.split("(?<![A-Z0-9])(?=[A-Z])|(?=[A-Z][^A-Z0-9])|(?=[0-9]([a-z]|[A-Z]{2}))");
    }

    void append(StringBuilder builder, String word, int index) {
      if (index > 0 || !ignoreFirst) {
        builder.append(capitalize(word));
      } else {
        builder.append(word);
      }
    }
  }
}
