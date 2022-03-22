package edu.pku.code2graph.xll.pattern;

import java.util.regex.Matcher;

public class Token {
  private final int start;
  private final int end;

  public final String name;
  public final String modifier;
  public final boolean isAnchor;
  public final boolean isGreedy;

  public Token(Matcher matcher) {
    this.start = matcher.start();
    this.end = matcher.end();
    this.name = matcher.group(1);
    this.modifier = matcher.group(2) != null ? matcher.group(2) : "auto";
    this.isAnchor = matcher.group(0).charAt(1) == '&';
    this.isGreedy = matcher.group(3) != null;
  }

  public String replace(String source, String capture, int offset) {
    return source.substring(offset, offset + start) + capture + source.substring(offset + end);
  }
}
