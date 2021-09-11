package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

public class Detector {
  private final Map<Language, Map<URI, List<Node>>> uriMap;
  private final Optional<Config> configOpt;

  public Detector(Map<Language, Map<URI, List<Node>>> uriMap, String path) {
    this.uriMap = uriMap;

    // load config
    ConfigLoader loader = new ConfigLoader();
    this.configOpt = loader.load(path);
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
        links.add(new Link(leftUri, rule, rightUri));
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
    this.configOpt.ifPresent(config -> {
      for (Rule rule : config.getRules()) {
        link(rule, links);
      }
    });

    return links;
  }
}
