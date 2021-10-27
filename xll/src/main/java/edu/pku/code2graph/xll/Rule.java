package edu.pku.code2graph.xll;

import java.util.List;
import java.util.Map;

public class Rule {
  public final URIPattern def;
  public final URIPattern use;

  public Rule(URIPattern def, URIPattern use) {
    this.def = def;
    this.use = use;
  }

  public Rule(Map<String, Object> rule) {
    this.def = new URIPattern((Map<String, Object>) rule.get("def"));
    this.def.isRef = false;
    this.use = new URIPattern((Map<String, Object>) rule.get("use"));
    this.use.isRef = true;
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
