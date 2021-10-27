package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class Linker {
  private final Set<URI> defs = new HashSet<>();
  private final Map<Language, Map<URI, List<Node>>> uriMap;
  private final Config config;
  private final static Logger logger = LoggerFactory.getLogger(Linker.class);

  public Linker(Map<Language, Map<URI, List<Node>>> uriMap, String path) {
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
    Map<Capture, List<URI>> defMap = scan(rule.def);
    Map<Capture, List<URI>> useMap = scan(rule.use);
    for (Capture capture : defMap.keySet()) {
      List<URI> uses = useMap.get(capture);
      if (uses == null) continue;
      List<URI> defs = defMap.get(capture);
      if (defs.size() > 1) {
        System.out.println("ambiguous xll found by " + capture.toString());
        System.out.println(formatUriList(defs));
        System.out.println(formatUriList(uses));
      }
      for (URI def : defs) {
        for (URI use : uses) {
          links.add(new Link(def, use, rule));
          for (Rule subRule : rule.subrules) {
            URIPattern newDef = new URIPattern(subRule.def);
            URIPattern newUse = new URIPattern(subRule.use);
            newDef.setFile(newDef.getFile());
            newUse.setFile(newUse.getFile());
            Rule newRule = new Rule(newDef, newUse, subRule.subrules);
            linkRule(newRule, links);
          }
        }
      }
      this.defs.addAll(defs);
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
