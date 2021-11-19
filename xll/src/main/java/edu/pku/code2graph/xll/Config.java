package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URITree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

public class Config {
  private final static Logger logger = LoggerFactory.getLogger(Config.class);

  private final List<String> presets;
  private final List<String> plugins;
  private final Map<String, Rule> rules;
  private final Map<String, List<String>> flowGraph;

  public static Config load(String path) throws FileNotFoundException {
    InputStream inputStream = new FileInputStream(path);
    Yaml yaml = new Yaml();
    Object config_raw = yaml.loadAll(inputStream).iterator().next();
    logger.debug("Using config from " + path);
    return new Config((Map<String, Object>) config_raw);
  }

  public Config() {
    presets = new ArrayList<>();
    plugins = new ArrayList<>();
    rules = new HashMap<>();
    flowGraph = new HashMap<>();
  }

  public Config(Map<String, Object> config) {
    presets = (List<String>) config.getOrDefault("presets", new ArrayList<>());
    plugins = (List<String>) config.getOrDefault("plugins", new ArrayList<>());
    flowGraph = (Map<String, List<String>>) config.get("flowgraph");
    Map<String, Object> rules_raw = (Map<String, Object>) config.get("rules");
    rules = new HashMap<>();
    for (Map.Entry<String, Object> entry : rules_raw.entrySet()) {
      rules.put(entry.getKey(), new Rule((Map<String, Object>) entry.getValue()));
    }
    logger.debug(toString());
  }

  public Map<String, List<String>> getFlowGraph() {
    return flowGraph;
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
    for (Map.Entry<String, List<String>> entry : flowGraph.entrySet()) {
      Rule rule = rules.get(entry.getKey());

      // collect all available contexts
      Set<Capture> localContext = new LinkedHashSet<>();
      for (String prevKey : entry.getValue()) {
        if (prevKey.equals("$")) continue;
        Set<Capture> globalContext = contexts.get(prevKey);
        localContext.addAll(globalContext);
      }

      // link rule for each context
      Linker linker = new Linker(rule, tree);
      for (Capture variables : localContext) {
        linker.link(variables);
      }
      links.addAll(linker.links);
      contexts.put(entry.getKey(), linker.captures);
    }

    logger.info("#xll = {}", links.size());
    return links;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Config {");
    for (Map.Entry<String, Rule> entry : rules.entrySet()) {
      entry.getKey();
      builder.append("\n  ");
      builder.append(entry.getKey());
      builder.append(": ");
      builder.append(entry.getValue());
    }
    builder.append("\n}");
    return builder.toString();
  }
}
