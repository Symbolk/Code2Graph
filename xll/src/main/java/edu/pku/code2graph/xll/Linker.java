package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class Linker {
  private final Map<Language, Map<URI, List<Node>>> uriMap;
  private final Config config;
  private final static Logger logger = LoggerFactory.getLogger(Linker.class);

  /**
   * visited def uris
   */
  private Set<URI> visited;

  /**
   * matched xlls
   */
  private List<Link> links;

  /**
   * variable contexts
   */
  private Map<String, Set<Capture>> contexts;

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
      if (visited.contains(uri)) continue;
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

  private void linkRule(Rule rule, Map<String, String> variables, Set<Capture> captures) {
    // TODO apply variables for rule

    // scan for use patterns
    Map<Capture, List<URI>> useMap = scan(rule.use);
    if (useMap.size() == 0) return;

    // scan for def patterns
    Map<Capture, List<URI>> defMap = scan(rule.def);
    for (Capture capture : defMap.keySet()) {
      // def capture should match use capture
      List<URI> uses = useMap.get(capture);
      if (uses == null) continue;

      // check ambiguous xlls
      List<URI> defs = defMap.get(capture);
      if (defs.size() > 1) {
        System.out.println("ambiguous xll found by " + capture.toString());
        System.out.println(formatUriList(defs));
        System.out.println(formatUriList(uses));
      }

      // update runtime properties
      captures.add(capture);
      for (URI def : defs) {
        for (URI use : uses) {
          links.add(new Link(def, use, rule));
        }
        visited.add(def);
      }
    }
  }

  public List<Link> linkAll() {
    // initialize runtime properties
    links = new ArrayList<>();
    contexts = new HashMap<>();
    visited = new HashSet<>();

    // create patterns and match
    for (Map.Entry<String, List<String>> entry : config.getFlow().entrySet()) {
      Rule rule = config.getRules().get(entry.getKey());
      Set<Capture> captures = new HashSet<>();
      contexts.put(entry.getKey(), captures);

      // collect all available contexts
      Set<Capture> localContext = new HashSet<>();
      for (String prevKey : entry.getValue()) {
        Set<Capture> globalContext = contexts.get(prevKey);
        localContext.addAll(globalContext);
      }

      // link rule for each context
      for (Capture variables : localContext) {
        linkRule(rule, variables, captures);
      }
    }

    logger.info("#xll = {}", links.size());
    return links;
  }
}
