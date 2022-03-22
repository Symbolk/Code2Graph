package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;

public abstract class AttributePattern {
  private final URIPattern root;

  public AttributePattern(final URIPattern root) {
    this.root = root;
  }

  public abstract Capture match(String target, Capture variables);
}
