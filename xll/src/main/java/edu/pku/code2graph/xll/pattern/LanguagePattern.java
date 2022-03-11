package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;

public class LanguagePattern implements AttributePattern {
  private final String source;

  public LanguagePattern(String source) {
    this.source = source;
  }

  public Capture match(String target, Capture variables) {
    if (source.equals("ANY")) return new Capture();
    if (target.equals("ANY")) return new Capture();
    if (source.equals(target)) return new Capture();
    return null;
  }
}
