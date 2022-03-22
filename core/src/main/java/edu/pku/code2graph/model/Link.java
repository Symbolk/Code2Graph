package edu.pku.code2graph.model;

import java.util.Objects;

public final class Link extends LinkBase<URI> {
  public Link(final URI def, final URI use, final String name, final boolean hidden) {
    super(def, use, name, hidden);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Link link = (Link) o;
    return Objects.equals(def, link.def) && Objects.equals(use, link.use);
  }
}
