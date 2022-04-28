package edu.pku.code2graph.model;

public final class Layer extends LayerBase {
  public Layer() {}

  public Layer(String identifier, Language language) {
    put("identifier", identifier);
    put("language", language.toString());
  }

  public Layer(String source) {
    String[] result = source.split("(?<!\\\\)\\[");
    String identifier = result[0];
    String attrs = source.substring(identifier.length() + 1, source.length() - 1);
    put("identifier", unescape(identifier));
    for (String attr : attrs.split("(?<!\\\\),")) {
      int position = attr.indexOf('=');
      assert position >= 0;
      put(attr.substring(0, position), unescape(attr.substring(position + 1)));
    }
  }

  @Override
  public Layer clone() {
    Layer layer = new Layer();
    layer.putAll(this);
    return layer;
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
