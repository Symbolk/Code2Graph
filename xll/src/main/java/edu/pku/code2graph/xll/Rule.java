package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Rule extends ArrayList<Object> {
  private URIPattern left;
  private URIPattern right;
  private List<Rule> subRules;

  public Rule() {}

  public Rule(List<Object> list) {
    super(list);
  }

  public Rule(URIPattern left, URIPattern right, List<Rule> subRules) {
    this.left = left;
    this.right = right;
    this.subRules = subRules;
  }

  public URIPattern getPattern(int index) {
    return new URIPattern((Map<String, Object>) get(index));
  }

  public URIPattern getLeft() {
    if (left != null) return left;
    return left = getPattern(0);
  }

  public URIPattern getRight() {
    if (right != null) return right;
    return right = getPattern(1);
  }

  public List<Rule> getSubRules() {
    if (subRules != null) return subRules;
    return subRules = subList(2, size()).stream().map(v -> new Rule((List<Object>) v)).toList();
  }
}
