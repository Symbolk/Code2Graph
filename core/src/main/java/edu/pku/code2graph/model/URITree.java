package edu.pku.code2graph.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class URITree  implements Serializable {
  public final Map<Layer, URITree> children = new HashMap<>();
  public final List<Node> nodes = new ArrayList<>();

  public URI uri;

  public List<Node> add(URI uri) {
    URITree root = this;
    for (Layer layer : uri.layers) {
      root = root.children.computeIfAbsent(layer, k -> new URITree());
    }
    root.uri = uri;
    return root.nodes;
  }

  public List<Node> add(String source) {
    return add(new URI(source));
  }

  public List<Node> get(URI uri) {
    URITree root = this;
    for (Layer layer : uri.layers) {
      root = root.children.get(layer);
      if (root == null) return null;
    }
    return root.nodes;
  }
}
