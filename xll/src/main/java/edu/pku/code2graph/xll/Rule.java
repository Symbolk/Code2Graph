package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;

import java.util.*;

public class Rule extends LinkBase<URIPattern> {
  public final List<String> deps;

  static private int index = 0;

  public Rule(final URIPattern def, final URIPattern use) {
    super(def, use, "#" + ++index);
    this.deps = new ArrayList<>();
    this.deps.add("$");
  }

  public Rule(final Map<String, Object> rule, final List<String> deps, final String name) {
    super(
      new URIPattern(false, (Map<String, Object>) rule.get("def")),
      new URIPattern(true, (Map<String, Object>) rule.get("use")),
      name,
      (boolean) rule.getOrDefault("hidden", false)
    );
    this.deps = deps;
  }

  public Link hydrate(Capture input, Capture output, Link target) {
    URI def = this.def.hydrate(input, output, target.def);
    URI use = this.use.hydrate(input, output, target.use);
    return new Link(def, use, this, input, output);
  }
}
