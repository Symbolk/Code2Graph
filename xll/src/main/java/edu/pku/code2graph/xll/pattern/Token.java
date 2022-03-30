package edu.pku.code2graph.xll.pattern;

import java.util.regex.Matcher;

public class Token {
  private final int start;
  private final int end;

  public final String name;
  public final String modifier;
  public final boolean isAnchor;

  public Token(Matcher matcher) {
    this.start = matcher.start();
    this.end = matcher.end();
    this.name = matcher.group(1);
    this.isAnchor = matcher.group(0).charAt(1) == '&';
    this.modifier = matcher.group(3) != null
      ? "slash"
      : matcher.group(2) != null
        ? matcher.group(2)
        : "auto";
  }

  public String replace(String source, String capture, int offset) {
    return source.substring(0, start - offset) + capture + source.substring(end - offset);
  }
}
