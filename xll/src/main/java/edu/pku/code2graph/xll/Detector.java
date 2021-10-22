package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Protocol;
import edu.pku.code2graph.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class Detector {
  private final Set<URI> defs = new HashSet<>();
  private final Map<Language, Map<URI, List<Node>>> uriMap;
  private final Config config;
  private final static Logger logger = LoggerFactory.getLogger(Detector.class);

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
    for (URI uri : uris.keySet()) {
      if (defs.contains(uri)) continue;
      Capture capture = pattern.match(uri);
      if (capture == null) continue;
      hashMap.computeIfAbsent(capture, k -> new ArrayList<>()).add(uri);
    }
    return hashMap;
  }

  private String formatUriList(List<URI> list) {
    Stream<String> segments = list.stream().map(uri -> uri.toString());
    return "[ " + String.join(",\n  ", segments.toArray(String[]::new)) + " ]";
  }

  public void linkRule(Rule rule, List<Link> links) {
    Map<Capture, List<URI>> leftMap = scan(rule.getLeft());
    Map<Capture, List<URI>> rightMap = scan(rule.getRight());
    for (Capture capture : leftMap.keySet()) {
      List<URI> rightUris = rightMap.get(capture);
      if (rightUris == null) continue;
      List<URI> leftUris = leftMap.get(capture);
      for (URI leftUri : leftUris) {
        for (URI rightUri : rightUris) {
          links.add(new Link(leftUri, rightUri, rule));
          for (Rule subRule : rule.getSubRules()) {
            URIPattern left = new URIPattern(subRule.getLeft());
            URIPattern right = new URIPattern(subRule.getRight());
            left.setFile(leftUri.getFile());
            right.setFile(rightUri.getFile());
            Rule newRule = new Rule(left, right, subRule.getSubRules());
            linkRule(newRule, links);
          }
        }
      }
      // workaround: pending object rule format
      if (rule.getRight().getProtocol() == Protocol.DEF) {
        defs.addAll(rightUris);
      }
    }
  }

  public List<Link> linkAll() {
    List<Link> links = new ArrayList<>();
    if (uriMap.isEmpty()) {
      return links;
    }

    // create patterns and match
    for (Rule rule : config.getRules()) {
      linkRule(rule, links);
    }

    logger.info("#xll = {}", links.size());
    return links;
  }
}
