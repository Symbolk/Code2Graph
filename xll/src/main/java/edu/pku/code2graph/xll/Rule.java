package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Stream;

public class Rule {
  public final URIPattern def;
  public final URIPattern use;

  /**
   * shared symbols for def / use patterns
   */
  public final Set<String> shared = new HashSet<>();

  public Rule(URIPattern def, URIPattern use) {
    this.def = def;
    this.use = use;
    initialize();
  }

  public Rule(Map<String, Object> rule) {
    def = new URIPattern(false, (Map<String, Object>) rule.get("def"));
    use = new URIPattern(true, (Map<String, Object>) rule.get("use"));
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
