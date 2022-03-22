package edu.pku.code2graph.model;

public class LinkBase<T extends URIBase> {
  public final T def;
  public final T use;
  public final String name;
  public final boolean hidden;

  public LinkBase(final T def, final T use, final String name, final boolean hidden) {
    this.def = def;
    this.use = use;
    this.name = name;
    this.hidden = hidden;
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
