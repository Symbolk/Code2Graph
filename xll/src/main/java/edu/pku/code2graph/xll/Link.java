package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;

import java.util.Objects;

public final class Link {
  public final URI def;
  public final URI use;
  public final String name;

  public Link(final URI def, final URI use, final String name) {
    this.def = def;
    this.name = name;
    this.use = use;
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    output.append("(");
    output.append(def.toString());
    output.append(", ");
    output.append(use.toString());
    output.append(")");
    return output.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Link link = (Link) o;
    return Objects.equals(def, link.def) && Objects.equals(use, link.use);
  }
}
