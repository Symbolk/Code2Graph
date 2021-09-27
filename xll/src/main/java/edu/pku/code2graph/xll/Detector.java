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

  private Map<Capture, List<URI>> scan(URIPattern pattern) {
    Map<Capture, List<URI>> hashMap = new HashMap<>();
    Map<URI, List<Node>> uris = uriMap.get(pattern.getLang());
    if (uris == null) return hashMap;
    for (URI uri: uris.keySet()) {
      Capture capture = pattern.match(uri);
      if (capture == null) continue;
      hashMap.computeIfAbsent(capture, k -> new ArrayList<>()).add(uri);
    }
    return hashMap;
  }

  public List<Link> link(Rule rule) {
    return link(rule, new ArrayList<>());
  }

  public List<Link> link(Rule rule, List<Link> links) {
    Map<Capture, List<URI>> leftMap = scan(rule.getLeft());
    Map<Capture, List<URI>> rightMap = scan(rule.getRight());
    for (Capture capture: leftMap.keySet()) {
      List<URI> rightUris = rightMap.get(capture);
      if (rightUris == null) continue;
      List<URI> leftUris = leftMap.get(capture);
      for (URI leftUri: leftUris) {
        for (URI rightUri: rightUris) {
          links.add(new Link(leftUri, rightUri, rule));
          for (Rule subRule : rule.getSubRules()) {
            URIPattern left = new URIPattern(subRule.getLeft());
            URIPattern right = new URIPattern(subRule.getRight());
            left.setFile(leftUri.getFile());
            right.setFile(rightUri.getFile());
            Rule newRule = new Rule(left, right, subRule.getSubRules());
            link(newRule, links);
          }
        }
      }
    }
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
