package edu.pku.code2graph.model;

import java.util.ArrayList;
import java.util.List;

public abstract class URIBase<T extends LayerBase> {
  public boolean isRef;

  protected List<T> layers = new ArrayList<>();

  public int getLayerCount() {
    return layers.size();
  }

  public T getLayer(int index) {
    return layers.get(index);
  }

  public void setLayer(int index, T layer) {
    layers.set(index, layer);
  }

  public void addLayer(T layer) {
    layers.add(layer);
  }

  public T addLayer(String identifier) {
    return addLayer(identifier, Language.ANY);
  }

  public abstract T addLayer(String identifier, Language language);

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    output.append(isRef ? "use" : "def");
    output.append(":");
    for (LayerBase layer : layers) {
      output.append("//").append(layer);
    }
    return output.toString();
  }
}
