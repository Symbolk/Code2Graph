package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;
import edu.pku.code2graph.xll.URIPattern;

public class LanguagePattern extends AttributePattern {
  private final String source;

  public LanguagePattern(String source, URIPattern root) {
    super(root);
    this.source = source;
  }

  public boolean match(String target, Capture variables, Capture result) {
    if (source.equals("ANY")) return true;
    if (target.equals("ANY")) return true;
    return source.equals(target);
  }
}
