package edu.pku.code2graph.model;

import edu.pku.code2graph.util.GraphUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    GraphUtil.getGraph().addVertex(node);
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

  public List<Node> getAllNodes() {
    URITree root = this;
    List<Node> res = new ArrayList<>(nodes);
    children.forEach((key, value) -> res.addAll(value.getAllNodes()));
    return res;
  }
}
