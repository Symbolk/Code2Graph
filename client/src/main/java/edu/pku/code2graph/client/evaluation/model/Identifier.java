package edu.pku.code2graph.client.evaluation.model;

import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.util.GraphUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Identifier {
  public static Map<String, Integer> uriCnt = new HashMap<>();
  private final String uri;
  private final Integer id;

  // optional
  private List<Node> node;

  public Identifier(String uri, Integer id) {
    this.uri = uri;
    this.id = id;
  }

  public Identifier(String uri, Integer id, List<Node> node) {
    this.uri = uri;
    this.id = id;
    this.node = node;
  }

  public String getUri() {
    return uri;
  }

  public Integer getId() {
    return id;
  }

  public List<Node> getNode() {
    return node;
  }

  @Override
  public int hashCode() {
    return uri.hashCode() * 10 + id.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Identifier)) return false;
    Identifier otherId = (Identifier) other;
    return otherId.uri.equals(uri) && otherId.id.equals(id);
  }

  // get all identifiers from uriTree
  public static List<Identifier> getAllIdentifiers() {
    List<Identifier> list = new ArrayList<>();
    addIdentifierFromURITree(list, GraphUtil.getUriTree());
    return list;
  }

  private static void addIdentifierFromURITree(List<Identifier> list, URITree tree) {
    if (tree.uri != null) {
      String uriStr = tree.uri.toString();
      if (!uriCnt.containsKey(uriStr)) {
        uriCnt.put(uriStr, 0);
      }
      int uriId = uriCnt.put(uriStr, uriCnt.get(uriStr) + 1);
      Identifier newId = new Identifier(URI.prettified(uriStr), uriId, tree.nodes);
      list.add(newId);
    }

    tree.children.forEach((key, value) -> addIdentifierFromURITree(list, value));
  }
}
