package edu.pku.code2graph.model;

public class LinkBase<T extends URIBase> {
  public final T def;
  public final T use;
  public final String name;

  public LinkBase(final T def, final T use, final String name) {
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
}
