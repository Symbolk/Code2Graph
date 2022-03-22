package edu.pku.code2graph.model;

public final class Layer extends LayerBase {
  public Layer(String identifier, Language language) {
    super();
    put("identifier", identifier);
    put("language", language.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return toString().equals(obj.toString());
  }
}
