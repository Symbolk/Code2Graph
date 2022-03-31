package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Link;
import edu.pku.code2graph.model.URITree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class Project {
  private final static Logger logger = LoggerFactory.getLogger(Project.class);

  private final Map<String, Rule> rules = new LinkedHashMap<>();

  public Project() {}

  static public Project load(String path) throws IOException {
    return new ConfigLoader(path).getProject();
  }

  public void addRule(Rule rule) {
    rules.put(rule.name, rule);
  }

  public List<Link> link(URITree tree) {
    // initialize runtime properties
    List<Link> links = new ArrayList<>();
    Map<String, Set<Capture>> contexts = new HashMap<>();

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
      Linker linker = new Linker(tree, rule);
      for (Capture variables : localContext) {
        linker.link(variables);
      }
      links.addAll(linker.links);
      contexts.put(name, linker.captures);
    }

    logger.info("#xll = {}", links.size());
    return links;
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
