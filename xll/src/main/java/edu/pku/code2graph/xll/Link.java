package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.Triple;

public final class Link extends Triple<URI, Rule, URI> {
  public final URI left;
  public final Rule middle;
  public final URI right;

  public Link(final URI left, final Rule middle, final URI right) {
    super();
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  @Override
  public URI getLeft() {
    return left;
  }

  @Override
  public Rule getMiddle() {
    return middle;
  }

  @Override
  public URI getRight() {
    return right;
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
