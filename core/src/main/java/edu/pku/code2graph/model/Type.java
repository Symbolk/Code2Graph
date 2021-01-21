package edu.pku.code2graph.model;

import java.io.Serializable;

/**
 * Type which contains label for node and edge
 * Produce for concrete languages.
 */
public class Type implements Serializable {
  private static final long serialVersionUID = 3672024400322609102L;

  public final String name;

  public Type(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  static class TypeFactory {
    protected TypeFactory() {}

    protected Type makeType(String name) {
      return new Type(name);
    }
  }
}
