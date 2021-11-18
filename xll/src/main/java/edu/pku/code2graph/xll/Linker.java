package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
   * matched links
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

  private Map<Capture, Pair<List<URI>, Set<Capture>>> scan(URIPattern pattern, Map<String, String> variables) {
    Map<Capture, Pair<List<URI>, Set<Capture>>> result = new HashMap<>();
    Map<URI, List<Node>> uris = uriMap.get(pattern.getLang());
    if (uris == null) return result;
    for (URI uri : uris.keySet()) {
      if (visited.contains(uri)) continue;
      Capture capture = pattern.match(uri, variables);
      if (capture == null) continue;
      Pair<List<URI>, Set<Capture>> pair = result
          .computeIfAbsent(capture, k -> new ImmutablePair<>(new ArrayList<>(), new HashSet<>()));
      pair.getLeft().add(uri);
      pair.getRight().add(capture);
    }
    return result;
  }

  private String formatUriList(List<URI> list) {
    Stream<String> segments = list.stream().map(uri -> uri.toString());
    return "[ " + String.join(",\n  ", segments.toArray(String[]::new)) + " ]";
  }

  private Set<Capture> linkRule(Rule rule, Capture variables) {
    Set<Capture> results = new HashSet<>();

    // scan for use patterns
    Map<Capture, Pair<List<URI>, Set<Capture>>> useMap = scan(rule.use, variables);
    if (useMap.size() == 0) return results;

    // scan for def patterns
    Map<Capture, Pair<List<URI>, Set<Capture>>> defMap = scan(rule.def, variables);
    for (Capture capture : defMap.keySet()) {
      // def capture should match use capture
      Pair<List<URI>, Set<Capture>> uses = useMap.get(capture);
      if (uses == null) continue;

      // check ambiguous links
      Pair<List<URI>, Set<Capture>> defs = defMap.get(capture);
      if (defs.getLeft().size() > 1) {
        System.out.println("ambiguous xll found by " + capture.toString());
        System.out.println(formatUriList(defs.getLeft()));
        System.out.println(formatUriList(uses.getLeft()));
      }

      // generate links
      for (URI use : uses.getLeft()) {
        for (URI def : defs.getLeft()) {
          links.add(new Link(def, use, rule));
        }
        visited.add(use);
      }

      // generate results
      for (Capture use : uses.getRight()) {
        for (Capture def : defs.getRight()) {
          Capture result = new Capture();
          result.putAll(variables);
          result.putAll(def);
          result.putAll(use);
          results.add(result);
        }
      }
    }

    return results;
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
        captures.addAll(linkRule(rule, variables));
      }
    }

    logger.info("#xll = {}", links.size());
    return links;
  }
}
