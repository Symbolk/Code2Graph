package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Link;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import org.apache.commons.lang3.tuple.Pair;
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

  /**
   * matched links
   */
  public final List<Link> links = new ArrayList<>();

  /**
   * matched captured
   */
  public final Set<Capture> captures = new HashSet<>();

  /**
   * visited use uris
   */
  public final Set<URI> visited;

  private String formatUriList(List<URI> list) {
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
        Pair<List<URI>, Set<Capture>> uses = useMap.get(useCap);

        // check ambiguous links
        Pair<List<URI>, Set<Capture>> defs = defMap.get(defCap);
        if (defs.getLeft().size() > 1) {
          System.out.println("ambiguous xll found by " + defCap);
          System.out.println(formatUriList(defs.getLeft()));
          System.out.println(formatUriList(uses.getLeft()));
        }

        // generate links
        for (URI use : uses.getLeft()) {
          for (URI def : defs.getLeft()) {
            links.add(new Link(def, use, rule.name, rule.hidden));
          }
          visited.add(use);
        }

        // generate results
        for (Capture use : uses.getRight()) {
          for (Capture def : defs.getRight()) {
            Capture result = variables.clone();
            // use def fragments to override use fragments
            result.putAll(use);
            result.putAll(def);
            captures.add(result);
          }
        }
      }
    }
  }
}
