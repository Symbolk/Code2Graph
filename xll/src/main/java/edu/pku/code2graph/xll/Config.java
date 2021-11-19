package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;
import edu.pku.code2graph.util.GraphUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Config {
  public Map<String, List<String>> flow;
  private List<String> presets;
  private String wordSep;
  private List<String> plugins;
  private Map<String, Rule> rules;
  private List<String> suppress;
  private final static Logger logger = LoggerFactory.getLogger(Config.class);

  public Config(Map<String, Object> config) {
    flow = (Map<String, List<String>>) config.get("flow");
    Map<String, Object> rules_raw = (Map<String, Object>) config.get("rules");
    rules = new HashMap<>();
    for (Map.Entry<String, Object> entry : rules_raw.entrySet()) {
      rules.put(entry.getKey(), new Rule((Map<String, Object>) entry.getValue()));
    }
    presets = (List<String>) config.getOrDefault("presets", new ArrayList<>());
    wordSep = (String) config.getOrDefault("word_sep", "");
    plugins = (List<String>) config.getOrDefault("plugins", new ArrayList<>());
    suppress = (List<String>) config.getOrDefault("suppress", new ArrayList<>());
  }

  public Map<String, List<String>> getFlow() {
    return flow;
  }

  public List<String> getPresets() {
    return presets;
  }

  public String getWord_sep() {
    return wordSep;
  }

  public List<String> getPlugins() {
    return plugins;
  }

  public Map<String, Rule> getRules() {
    return rules;
  }

  public List<String> getSuppress() {
    return suppress;
  }

  public void setFlow(Map<String, List<String>> flow) {
    this.flow = flow;
  }

  public void setPresets(List<String> presets) {
    this.presets = presets;
  }

  public void setWordSep(String word_sep) {
    this.wordSep = word_sep;
  }

  public void setPlugins(List<String> plugins) {
    this.plugins = plugins;
  }

  public void setRules(Map<String, Rule> rules) {
    this.rules = rules;
  }

  public void setSuppress(List<String> suppress) {
    this.suppress = suppress;
  }

  public List<Link> link(URITree tree) {
    // initialize runtime properties
    List<Link> links = new ArrayList<>();
    Map<String, Set<Capture>> contexts = new HashMap<>();

    // create patterns and match
    for (Map.Entry<String, List<String>> entry : getFlow().entrySet()) {
      Rule rule = getRules().get(entry.getKey());

      // collect all available contexts
      Set<Capture> localContext = new HashSet<>();
      for (String prevKey : entry.getValue()) {
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
    return "Config{"
        + "presets="
        + presets
        + ", word_sep='"
        + wordSep
        + '\''
        + ", plugins="
        + plugins
        + ", rules="
        + rules
        + ", suppress="
        + suppress
        + '}';
  }
}
