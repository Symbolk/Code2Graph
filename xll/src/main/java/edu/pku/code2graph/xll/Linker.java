package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class Linker {
  private final static Logger logger = LoggerFactory.getLogger(Linker.class);

  public final Rule rule;
  public final URITree tree;

  private final Scanner def;
  private final Scanner use;

  public Linker(URITree tree, Rule rule) {
    this.rule = rule;
    this.tree = tree;
    this.def = new Scanner(rule.def, this);
    this.use = new Scanner(rule.use, this);
  }

  public Linker(URITree tree, URIPattern def, URIPattern use) {
    this.rule = new Rule(def, use);
    this.tree = tree;
    this.def = new Scanner(def, this);
    this.use = new Scanner(use, this);
  }

  /**
   * matched links
   */
  public final List<Link> links = new ArrayList<>();

  /**
   * matched captured
   */
  public final Set<Capture> context = new HashSet<>();

  /**
   * visited use uris
   */
  public final Set<URI> visited = new HashSet<>();

  private String formatUriList(Set<URI> list) {
    Stream<String> segments = list.stream().map(uri -> uri.toString());
    return "[ " + String.join(",\n  ", segments.toArray(String[]::new)) + " ]";
  }

  public void link() {
    link(new Capture());
  }

  public void link(Capture variables) {
    // scan for use patterns
    Scanner.Result useMap = this.use.scan(variables);
    if (useMap.size() == 0) return;

    // scan for def patterns
    Scanner.Result defMap = this.def.scan(variables);
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
            Capture result = variables.clone();
            result.putAll(use.getValue());
            result.putAll(def.getValue());
            context.add(result);
            links.add(new Link(def.getKey(), use.getKey(), rule, result));
          }
          visited.add(use.getKey());
        }
      }
    }
  }
}
