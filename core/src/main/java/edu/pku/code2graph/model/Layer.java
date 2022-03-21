package edu.pku.code2graph.model;

public class Layer extends LayerBase {
  public Layer(String identifier, Language language) {
    super(identifier, language);
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
