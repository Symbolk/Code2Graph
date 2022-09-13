package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class Linker {
  private static final Logger logger = LoggerFactory.getLogger(Linker.class);

  public final Rule rule;
  public final URITree tree;

  private final Scanner def;
  private final Scanner use;

  public Linker(URITree tree, Rule rule, Set<URI> visited) {
    this.rule = rule;
    this.tree = tree;
    this.def = new Scanner(rule.def, this);
    this.use = new Scanner(rule.use, this);
    this.visited = visited;
  }

  public Linker(URITree tree, URIPattern def, URIPattern use) {
    this(tree, new Rule(def, use), new HashSet<>());
  }

  /** matched links */
  public final List<Link> links = new ArrayList<>();

  /** matched captured */
  public final Set<Capture> context = new HashSet<>();

  /** visited use uris */
  public final Set<URI> visited;

  private String formatUriList(Set<URI> list) {
    Stream<String> segments = list.stream().map(uri -> uri.toString());
    return "[ " + String.join(",\n  ", segments.toArray(String[]::new)) + " ]";
  }

  public void link() {
    link(new Capture(), null);
  }

  public void link(Capture input, Set<URI> useSet) {
    // scan for use patterns
    Map<Capture, Map<URI, Capture>> useMap = this.use.scan(input);
    if (useSet != null) {
      useSet.addAll(
              useMap.values().stream()
                      .map(Map::keySet).reduce(new HashSet<>(), (keySet, newSet) -> {
                        keySet.addAll(newSet);
                        return keySet;
                      }));
    }
    if (useMap.size() == 0) return;

    // scan for def patterns
    Map<Capture, Map<URI, Capture>> defMap = this.def.scan(input);
    for (Capture defCap : defMap.keySet()) {
      // def capture should match use capture
      for (Capture useCap : useMap.keySet()) {
        if (!defCap.match(useCap)) continue;
        Map<URI, Capture> uses = useMap.get(useCap);

        // check ambiguous links
        Map<URI, Capture> defs = defMap.get(defCap);
        if (defs.size() > 1) {
          System.out.println("ambiguous xll found by " + defCap);
          System.out.println(formatUriList(defs.keySet()));
          System.out.println(formatUriList(uses.keySet()));
        }

        // generate links and update context
        for (Map.Entry<URI, Capture> use : uses.entrySet()) {
          for (Map.Entry<URI, Capture> def : defs.entrySet()) {
            // def fragments should override use fragments
            Capture output = input.clone();
            output.putAll(use.getValue());
            output.putAll(def.getValue());
            context.add(output);
            links.add(new Link(def.getKey(), use.getKey(), rule, input, output));
          }
          visited.add(use.getKey());
        }
      }
    }
  }
}
