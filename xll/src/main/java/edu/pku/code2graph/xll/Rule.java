package edu.pku.code2graph.xll;

import java.util.*;

public class Rule {
  public final URIPattern def;
  public final URIPattern use;
  public final List<String> deps;

  /**
   * shared symbols for def / use patterns
   */
  public final Set<String> shared = new HashSet<>();

  public Rule(URIPattern def, URIPattern use) {
    this.def = def;
    this.use = use;
    this.deps = new ArrayList<>();
    initialize();
  }

  public Rule(Map<String, Object> rule, List<String> deps) {
    this.def = new URIPattern(false, (Map<String, Object>) rule.get("def"));
    this.use = new URIPattern(true, (Map<String, Object>) rule.get("use"));
    this.deps = deps;
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
