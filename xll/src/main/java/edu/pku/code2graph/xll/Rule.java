package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Rule {
  public final URIPattern def;
  public final URIPattern use;
  public final List<Rule> subrules;

  public Rule(URIPattern def, URIPattern use, List<Rule> subrules) {
    this.def = def;
    this.use = use;
    this.subrules = subrules;
  }

  public Rule(Map<String, Object> rule) {
    this.def = (URIPattern) rule.get("def");
    this.def.isRef = false;
    this.use = (URIPattern) rule.get("use");
    this.def.isRef = true;
    this.subrules = (List<Rule>) rule.getOrDefault("subrules", new ArrayList());
  }
}
