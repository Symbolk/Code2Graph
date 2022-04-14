package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class Project {
  private final static Logger logger = LoggerFactory.getLogger(Project.class);

  private final Map<String, Rule> rules = new LinkedHashMap<>();

  private URITree tree;

  // runtime properties
  private final List<Link> links = new ArrayList<>();
  private final Map<String, Set<Capture>> contexts = new HashMap<>();
  private final Set<URI> visited = new HashSet<>();

  public Project() {}

  static public Project load(String path) throws IOException {
    return new ConfigLoader(path).getProject();
  }

  public void addRule(Rule rule) {
    rules.put(rule.name, rule);
  }

  public void setTree(URITree tree) {
    this.tree = tree;
  }

  public List<Link> link() {
    // create patterns and match
    for (Map.Entry<String, Rule> entry : rules.entrySet()) {
      String name = entry.getKey();
      Rule rule = entry.getValue();

      // collect all available contexts
      Set<Capture> localContext = new LinkedHashSet<>();
      for (String prev : rule.deps) {
        if (prev.equals("$")) {
          localContext.add(new Capture());
        } else {
          localContext.addAll(contexts.get(prev));
        }
      }

      // link rule for each context
      logger.debug("Linking " + rule);
      Linker linker = new Linker(tree, rule, visited);
      for (Capture variables : localContext) {
        linker.link(variables);
      }
      links.addAll(linker.links);
      contexts.put(name, linker.context);
    }

    logger.info("#xll = {}", links.size());
    return links;
  }

  public void rename(URI oldUri, URI newUri, Map<URI, Set<URI>> changes) {
    for (Link link : links) {
      if (link.def.equals(oldUri)) {
        Capture o = link.rule.def.match(newUri, link.input);
        if (o == null) continue;
        Capture output = link.output.clone();
        output.putAll(o);
        URI oldUri2 = link.use;
        URI newUri2 = link.rule.use.refactor(oldUri2, link.input, output);
        changes.computeIfAbsent(oldUri2, k -> new HashSet<>()).add(newUri2);
        link.use = newUri2;
        rename(oldUri2, newUri2, changes);
      } else if (link.use.equals(oldUri)) {
        Capture o = link.rule.use.match(newUri, link.input);
        if (o == null) continue;
        Capture output = link.output.clone();
        output.putAll(o);
        URI oldUri2 = link.def;
        URI newUri2 = link.rule.def.refactor(oldUri2, link.input, output);
        link.def = newUri2;
        changes.computeIfAbsent(oldUri2, k -> new HashSet<>()).add(newUri2);
        rename(oldUri2, newUri2, changes);
      }
    }
  }

  public List<Pair<URI, URI>> rename(URI oldUri, URI newUri) {
    Map<URI, Set<URI>> changes = new HashMap<>();
    rename(oldUri, newUri, changes);

    List<Pair<URI, URI>> result = new ArrayList<>();
    for (URI source : changes.keySet()) {
      Set<URI> targets = changes.get(source);
      if (targets.size() > 1) {
        System.out.println("ambiguous rename: " + source);
        System.out.println(targets);
      }
      URI target = (URI) targets.toArray()[0];
      result.add(new ImmutablePair<>(source, target));
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Config {");
    for (Map.Entry<String, Rule> entry : rules.entrySet()) {
      builder.append("\n  ");
      builder.append(entry.getKey());
      builder.append(": ");
      builder.append(entry.getValue());
    }
    builder.append("\n}");
    return builder.toString();
  }
}
