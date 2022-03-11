package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;

public interface AttributePattern {
  Capture match(String target, Capture variables);
}
