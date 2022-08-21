package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.Pair;

public class Change {
  public final String identifier;
  public final String source;
  public final String language;

  Change(String source) {
    this.source = source;
    URI uri = new URI(source);
    Layer last = uri.layers.get(uri.layers.size() - 1);
    String identifier = Credit.getLastSegment(last.get("identifier"));
    if (uri.layers.size() == 1) {
      Pair<String, String> division = Credit.splitExtension(identifier);
      language = division.getRight().toUpperCase();
      this.identifier = division.getLeft();
    } else {
      language = uri.layers.get(1).get("language");
      this.identifier = identifier;
    }
  }
}
