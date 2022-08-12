package edu.pku.code2graph.model;

import edu.pku.code2graph.util.GraphUtil;

import java.io.Serializable;
import java.util.*;

public class URITree implements Serializable {
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

  public List<Node> add(String source, Range range) {
    URI uri = new URI(source);
    ElementNode node =
        new ElementNode(
            GraphUtil.nid(),
            uri.getLayer(uri.getLayerCount() - 1).getLanguage(),
            null,
            "",
            "",
            "",
            uri);
    node.setRange(range);
    add(new URI(source)).add(node);

    return nodes;
  }

  public boolean has(URI uri) {
    return get(uri) != null;
  }

  public List<Node> get(URI uri) {
    URITree root = this;
    for (Layer layer : uri.layers) {
      root = root.children.get(layer);
      if (root == null) return null;
    }
    return root.nodes;
  }

  public Set<URI> keySet() {
    Set<URI> result = new HashSet<>();
    if (uri != null) result.add(uri);
    children.forEach((key, value) -> {
      result.addAll(value.keySet());
    });
    return result;
  }

  public List<Node> getAllNodes() {
    List<Node> result = new ArrayList<>(nodes);
    children.forEach((key, value) -> {
      result.addAll(value.getAllNodes());
    });
    return result;
  }
}
