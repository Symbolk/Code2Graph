package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URITree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
  private final static Logger logger = LoggerFactory.getLogger(Config.class);

  private final List<String> presets;
  private final List<String> plugins;
  private final Map<String, Rule> rules = new LinkedHashMap<>();
  private final Set<String> missing = new HashSet<>();

  public Config() {
    presets = new ArrayList<>();
    plugins = new ArrayList<>();
  }

  public Config(Map<String, Object> config) {
    presets = (List<String>) config.getOrDefault("presets", new ArrayList<>());
    plugins = (List<String>) config.getOrDefault("plugins", new ArrayList<>());
    Map<String, List<String>> flowGraph = (Map<String, List<String>>) config.get("flowgraph");
    Map<String, Object> rawRules = (Map<String, Object>) config.get("rules");
    toposort(flowGraph, rawRules);
    logger.debug(toString());
  }

  private void reportMissing(String name) {
    if (missing.contains(name)) return;
    logger.warn("missing rule definition {}", name);
    missing.add(name);
  }

  private void toposort(Map<String, List<String>> flowGraph, Map<String, Object> rawRules) {
    Map<String, Integer> degrees = new HashMap<>();
    for (String name : rawRules.keySet()) {
      if (!flowGraph.containsKey(name)) {
        logger.warn("unused rule definition {}", name);
      }
    }

    for (String name : flowGraph.keySet()) {
      flowGraph.put(name, flowGraph.get(name).stream().filter(x -> {
        if (rawRules.containsKey(x) || x.equals("$")) return true;
        reportMissing(x);
        return false;
      }).collect(Collectors.toList()));

      if (rawRules.containsKey(name)) {
        degrees.put(name, 0);
      } else {
        reportMissing(name);
      }
    }

    for (String name : flowGraph.keySet()) {
      for (String prev : flowGraph.get(name)) {
        if (prev.equals("$")) continue;
        degrees.put(prev, degrees.get(prev) + 1);
      }
    }

    List<String> queue = new ArrayList<>();
    for (String name : degrees.keySet()) {
      if (degrees.get(name) == 0) {
        queue.add(name);
      }
    }

    int index = 0;
    while (index < queue.size()) {
      String name = queue.get(index);
      for (String prev : flowGraph.get(name)) {
        if (prev.equals("$")) continue;
        int degree = degrees.get(prev) - 1;
        degrees.put(prev, degree);
        if (degree == 0) {
          queue.add(prev);
        }
      }
      index += 1;
    }

    index -= 1;
    while (index >= 0) {
      String name = queue.get(index);
      List<String> deps = flowGraph.get(name);
      Rule rule = new Rule((Map<String, Object>) rawRules.get(name), deps, name);
      rules.put(name, rule);
      index -= 1;
    }
  }

  public List<String> getPresets() {
    return presets;
  }

  public List<String> getPlugins() {
    return plugins;
  }

  public Map<String, Rule> getRules() {
    return rules;
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
      logger.debug("Linking " + rule.toString());
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

  public static Config load(String path) throws FileNotFoundException {
    InputStream inputStream = new FileInputStream(path);
    Yaml yaml = new Yaml();
    Object rawConfig = yaml.loadAll(inputStream).iterator().next();
    logger.debug("load from " + path);
    return new Config((Map<String, Object>) rawConfig);
  }
}
