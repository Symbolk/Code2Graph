package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;
import edu.pku.code2graph.xll.URIPattern;

public class LanguagePattern extends AttributePattern {
  private final String source;

  public LanguagePattern(String source, URIPattern root) {
    super(root);
    this.source = source;
  }

  public Capture match(String target, Capture variables) {
    if (source.equals("ANY")) return new Capture();
    if (target.equals("ANY")) return new Capture();
    if (source.equals(target)) return new Capture();
    return null;
  }
}
