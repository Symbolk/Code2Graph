package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Project {
  private final static Logger logger = LoggerFactory.getLogger(Project.class);

  private final Map<String, Rule> rules = new LinkedHashMap<>();

  private URITree tree;

  // runtime properties
  private List<Link> links = new ArrayList<>();
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

  private URI infer(URI oldUri, URI newUri, URI contra, URIPattern cis, URIPattern trans, Capture input) {
    Capture oldCap = cis.match(oldUri, input);
    Capture newCap = cis.match(newUri, input);
    Capture contraCap = trans.match(contra, input);
    if (newCap == null) return null;
    Capture output = input.clone();
    output.putAll(contraCap);
    output.putAll(newCap);
    for (String key : newCap.keySet()) {
      Fragment oldFrag = oldCap.get(key);
      Fragment newFrag = newCap.get(key);
      Fragment contraFrag = contraCap.get(key);
      if (contraFrag != null) {
        newFrag.align(oldFrag, contraFrag);
      }
    }
    return trans.refactor(contra, input, output);
  }

  public void rename(URI oldUri, URI newUri, Map<URI, Set<URI>> changes) {
    if (!changes.computeIfAbsent(oldUri, k -> new HashSet<>()).add(newUri)) return;
    for (Link link : links) {
      if (link.def.equals(oldUri)) {
        URI contra = link.use;
        URI result = infer(oldUri, newUri, contra, link.rule.def, link.rule.use, link.input);
        if (result == null) continue;
        link.use = result;
        rename(contra, result, changes);
      } else if (link.use.equals(oldUri)) {
        URI contra = link.def;
        URI result = infer(oldUri, newUri, contra, link.rule.use, link.rule.def, link.input);
        if (result == null) continue;
        link.def = result;
        rename(contra, result, changes);
      }
    }
  }

  public List<Pair<URI, URI>> rename(URI oldUri, URI newUri) {
    Map<URI, Set<URI>> changes = new HashMap<>();
    List<Link> links = this.links.stream().map(Link::clone).collect(Collectors.toList());

    rename(oldUri, newUri, changes);
    changes.remove(oldUri);
    this.links = links;
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
