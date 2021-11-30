package edu.pku.code2graph.xll;

import java.util.*;

public class Rule {
  public final URIPattern def;
  public final URIPattern use;
  public final List<String> deps;
  public final String name;

  static private int index = 0;

  /**
   * shared symbols for def / use patterns
   */
  public final Set<String> shared = new HashSet<>();

  public Rule(URIPattern def, URIPattern use) {
    this.def = def;
    this.use = use;
    this.deps = new ArrayList<>();
    this.name = "#" + String.valueOf(++index);
    initialize();
  }

  public Rule(Map<String, Object> rule, List<String> deps, String name) {
    this.def = new URIPattern(false, (Map<String, Object>) rule.get("def"));
    this.use = new URIPattern(true, (Map<String, Object>) rule.get("use"));
    this.deps = deps;
    this.name = name;
    initialize();
  }

  private void initialize() {
    for (String name : def.symbols) {
      if (use.symbols.contains(name)) {
        shared.add(name);
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    output.append("(");
    output.append(def.toString());
    output.append(", ");
    output.append(use.toString());
    output.append(")");
    return output.toString();
  }
}
