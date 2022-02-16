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
  private final String lang;

  // optional
  private Node node;

  public Identifier(String uri, Integer id, String lang) {
    this.uri = uri;
    this.id = id;
    this.lang = lang;
  }

  public Identifier(String uri, Integer id, Node node) {
    this.uri = uri;
    this.id = id;
    this.node = node;
    this.lang = node.getLanguage().toString();
  }

  public String getUri() {
    return uri;
  }

  public Integer getId() {
    return id;
  }

  public Node getNode() {
    return node;
  }

  public String getLang() {
    return lang;
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
      int nodeCnt = tree.nodes.size();
      int nowIdCnt = 0;
      if (uriCnt.containsKey(uriStr)) {
        nowIdCnt = uriCnt.get(uriStr);
      }
      for (int i = 0; i < nodeCnt; i++) {
        Identifier newId = new Identifier(URI.prettified(uriStr), nowIdCnt + i, tree.nodes.get(i));
        list.add(newId);
      }
      if (!uriCnt.containsKey(uriStr)) {
        uriCnt.put(uriStr, nodeCnt);
      } else {
        uriCnt.put(uriStr, nowIdCnt + nodeCnt);
      }
    }

    tree.children.forEach((key, value) -> addIdentifierFromURITree(list, value));
  }
}
