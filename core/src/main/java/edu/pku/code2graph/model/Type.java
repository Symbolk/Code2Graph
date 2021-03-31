package edu.pku.code2graph.model;

import java.io.Serializable;

/**
 * Type which contains label for node and edge, dynamically produce for concrete languages when
 * needed.
 */
public class Type implements Serializable {
  private static final long serialVersionUID = 3672024400322609102L;

  public final String name; // name of the type (unique in one run)
  public final boolean isEntity; // is entity level type or not (determined in language, only for element nodes)

  public Type(String name) {
    this.name = name;
    this.isEntity = false;
  }

  public Type(String name, boolean isEntity) {
    this.name = name;
    this.isEntity = isEntity;
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

    protected Type makeType(String name, boolean isEntity) {
      return new Type(name, isEntity);
    }
  }
}
