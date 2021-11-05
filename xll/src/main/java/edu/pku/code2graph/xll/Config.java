package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
  public Map<String, List<String>> flow;
  private List<String> presets;
  private String wordSep;
  private List<String> plugins;
  private Map<String, Rule> rules;
  private List<String> suppress;

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
