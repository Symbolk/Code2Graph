package edu.pku.code2graph.model;

public final class Layer extends LayerBase {
  public Layer(String identifier, Language language) {
    super();
    put("identifier", identifier);
    put("language", language.toString());
  }

  public Layer(String source) {
    int splitPoint = source.indexOf('[');
    String identifier = source.substring(0, splitPoint);
    String attrs = source.substring(splitPoint + 1, source.length() - 1);
    put("identifier", unescape(identifier));
    for (String attr : attrs.split("(?<!\\\\),")) {
      int position = attr.indexOf('=');
      assert position >= 0;
      put(attr.substring(0, position), unescape(attr.substring(position + 1)));
    }
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
