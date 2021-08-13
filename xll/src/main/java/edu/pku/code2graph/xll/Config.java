package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.List;

public class Config {
  private List<String> presets;
  private String word_sep;
  private List<String> plugins;
  // FIXME: represent rules as objects
  private List<Rule> rules;
  private List<String> suppress;

  public Config() {
    presets = new ArrayList<>();
    word_sep = "";
    plugins = new ArrayList<>();
    rules = new ArrayList<>();
    suppress = new ArrayList<>();
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
