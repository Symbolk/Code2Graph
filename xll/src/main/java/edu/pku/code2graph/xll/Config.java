package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Config {
  public List<List<Step>> jobs;
  private List<String> presets;
  private String word_sep;
  private List<String> plugins;
  private List<Rule> rules;
  private List<String> suppress;

  public Config(Map<String, Object> config) {
    Object jobs_raw = config.getOrDefault("rules", new ArrayList<>());
    jobs = ((List<List<Map<String, Object>>>) jobs_raw).stream().map(job -> job.stream().map(Step::new).toList()).toList();
    Object rules_raw = config.getOrDefault("rules", new ArrayList<>());
    rules = ((List<Map<String, Object>>) rules_raw).stream().map(Rule::new).toList();
    presets = (List<String>) config.getOrDefault("presets", new ArrayList<>());
    word_sep = (String) config.getOrDefault("word_sep", "");
    plugins = (List<String>) config.getOrDefault("plugins", new ArrayList<>());
    suppress = (List<String>) config.getOrDefault("suppress", new ArrayList<>());
  }

  public List<List<Step>> getJobs() {
    return jobs;
  }

  public void setJobs(List<List<Step>> jobs) {
    this.jobs = jobs;
  }

  public List<String> getPresets() {
    return presets;
  }

  public String getWord_sep() {
    return word_sep;
  }

  public List<String> getPlugins() {
    return plugins;
  }

  public List<Rule> getRules() {
    return rules;
  }

  public List<String> getSuppress() {
    return suppress;
  }

  public void setPresets(List<String> presets) {
    this.presets = presets;
  }

  public void setWord_sep(String word_sep) {
    this.word_sep = word_sep;
  }

  public void setPlugins(List<String> plugins) {
    this.plugins = plugins;
  }

  public void setRules(List<Rule> rules) {
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
        + word_sep
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
