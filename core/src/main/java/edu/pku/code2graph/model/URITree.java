package edu.pku.code2graph.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class URITree extends HashMap<Language, Map<URI, List<Node>>> {
  public void add(String source) {
    URI uri = new URI(source);
    add(uri.getLang(), uri);
  }

  public List<Node> add(Language language, URI uri) {
    return this
        .computeIfAbsent(language, k -> new HashMap<>())
        .computeIfAbsent(uri, k -> new ArrayList<>());
  }

  /**
   * Add one single uri to the uri tree
   *
   * @param language
   * @param uri
   * @param node
   */
  public void add(Language language, URI uri, Node node) {
    this.add(language, uri).add(node);
  }
}
