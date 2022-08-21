package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Candidate {
  public final String source1;
  public final String source2;
  public final String pattern1;
  public final String pattern2;
  public final double similarity;

  public Candidate(Change change1, Change change2, Comparison comparison) {
    this.source1 = change1.source;
    this.source2 = change2.source;
    this.pattern1 = pattern(source1, comparison.getPattern1());
    this.pattern2 = pattern(source2, comparison.getPattern2());
    this.similarity = comparison.similarity;
  }

  private String pattern(String source, String identifier) {
    URI uri = new URI(source);
    if (uri.layers.size() == 1) {
      Layer layer = uri.layers.get(0);
      Pair<String, String> division = Credit.splitExtension(layer.get("identifier"));
      String extension = division.getRight();
      layer.put("identifier", identifier + "." + extension);
    } else {
      for (int i = 0; i < uri.layers.size(); i++) {
        Layer layer = uri.layers.get(i);
        if (i == uri.layers.size() - 1) {
          layer.put("identifier", identifier);
        } else {
          layer.put("identifier", "**");
        }
      }
    }
    return uri.toString();
  }

  @Override
  public String toString() {
    return pattern1 + "," + pattern2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
