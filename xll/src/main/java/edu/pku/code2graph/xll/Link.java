package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;

public final class Link {
  public final URI left;
  public final Rule middle;
  public final URI right;

  public Link(final URI left, final Rule middle, final URI right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    output.append("(");
    output.append(left.toString());
    output.append(", ");
    output.append(right.toString());
    output.append(")");
    return output.toString();
  }
}
