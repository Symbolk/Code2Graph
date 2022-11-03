package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;

public final class Link extends LinkBase<URI> {
  public final Capture input;
  public final Capture output;
  public final Rule rule;
  public boolean modified = false;

  public Link(
      final URI def, final URI use, final Rule rule, final Capture input, final Capture output) {
    super(def, use, rule.name, rule.hidden, rule.brokenType);
    this.rule = rule;
    this.input = input;
    this.output = output;
  }

  public Link(final URI def, final URI use) {
    super(def, use, null);
    this.def = def;
    this.use = use;
    this.input = null;
    this.output = null;
    this.rule = null;
  }

  @Override
  public Link clone() {
    return new Link(def, use, rule, input, output);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }
}
