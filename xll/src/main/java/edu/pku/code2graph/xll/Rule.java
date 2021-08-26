package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.Map;

public class Rule extends ArrayList<Map<String, Object>> {
  private URIPattern left;
  private URIPattern right;

  public URIPattern getLeft() {
    if (left != null) return left;
    return left = new URIPattern(get(0));
  }

  public URIPattern getRight() {
    if (right != null) return right;
    return right = new URIPattern(get(1));
  }
}
