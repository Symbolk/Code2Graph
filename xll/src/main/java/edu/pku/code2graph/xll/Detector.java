package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;

import java.util.*;

public class Detector {
  private final Map<Language, Map<URI, List<Node>>> uriMap;
  private final Config config;

  public Detector(Map<Language, Map<URI, List<Node>>> uriMap, String path) {
    this.uriMap = uriMap;

    // load config
    ConfigLoader loader = new ConfigLoader();
    config = loader.load(path).get();
  }

  private interface MatchCallback {
    void action(URI uri, Map<String, String> captures);
  }

  private void match(URIPattern pattern, MatchCallback callback) {
    Map<URI, List<Node>> uris = uriMap.get(pattern.getLang());
    if (uris == null) return;
    for (URI uri: uris.keySet()) {
      Map<String, String> captures = pattern.match(uri);
      if (captures == null) continue;
      callback.action(uri, captures);
    }
  }

  public List<Link> link(Rule rule) {
    return link(rule, new ArrayList<>());
  }

  public List<Link> link(Rule rule, List<Link> links) {
    match(rule.getLeft(), (leftUri, leftCaps) -> {
      URIPattern pattern = rule.getRight().applyCaptures(leftCaps);
      match(pattern, (rightUri, rightCaps) -> {
        links.add(new Link(leftUri, rightUri, rule));
        for (Rule subRule : rule.getSubRules()) {
          URIPattern left = new URIPattern(subRule.getLeft());
          URIPattern right = new URIPattern(subRule.getRight());
          left.setFile(leftUri.getFile());
          right.setFile(rightUri.getFile());
          Rule newRule = new Rule(left, right, subRule.getSubRules());
          link(newRule, links);
        }
      });
    });
    return links;
  }

  public List<Link> linkAll() {
    List<Link> links = new ArrayList<>();
    if (uriMap.isEmpty()) {
      return links;
    }

    // create patterns and match
    for (Rule rule : config.getRules()) {
      link(rule, links);
    }

    return links;
  }
}
