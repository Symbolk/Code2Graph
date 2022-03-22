package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.model.LinkBase;

import java.util.*;

public class Rule extends LinkBase<URIPattern> {
  public final List<String> deps;

  static private int index = 0;

  public Rule(final URIPattern def, final URIPattern use) {
    super(def, use, "#" + ++index);
    this.deps = new ArrayList<>();
  }

  public Rule(final Map<String, Object> rule, final List<String> deps, final String name) {
    super(
      new URIPattern(false, (Map<String, Object>) rule.get("def")),
      new URIPattern(true, (Map<String, Object>) rule.get("use")),
      name
    );
    this.deps = deps;
  }
}
