package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;
import edu.pku.code2graph.xll.URIPattern;

public class LanguagePattern extends AttributePattern {
  private final String source;

  public LanguagePattern(String name, String source, URIPattern root) {
    super(name, root, 0);
    this.source = source;
  }

  public boolean match(String target, Capture variables, Capture result, boolean ignoreAnchors) {
    if (source.equals("ANY")) return true;
    if (target.equals("ANY")) return true;
    return source.equals(target);
  }

  public String refactor(String target, Capture input, Capture output) {
    return target;
  }
}
