package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.Map;

public class Rule extends ArrayList<Map<String, Object>> {
  private URIPattern left;
  private URIPattern right;

  public URIPattern getIndex(int index) {
    return new URIPattern(get(index));
  }

  public URIPattern getLeft() {
    if (left != null) return left;
    return left = getIndex(0);
  }

  public URIPattern getRight() {
    if (right != null) return right;
    return right = getIndex(1);
  }
}
