package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;

import java.util.Objects;

public final class Link extends LinkBase<URI> {
  public final Capture capture;

  public Link(final URI def, final URI use, final String name) {
    super(def, use, name, false);
    this.capture = null;
  }

  public Link(final URI def, final URI use, final Rule rule, final Capture capture) {
    super(def, use, rule.name, rule.hidden);
    this.capture = capture;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Link link = (Link) o;
    return Objects.equals(def, link.def) && Objects.equals(use, link.use);
  }
}
