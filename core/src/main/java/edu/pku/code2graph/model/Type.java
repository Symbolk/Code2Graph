package edu.pku.code2graph.model;

public class Type {
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
