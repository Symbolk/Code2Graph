package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URILike;

import java.util.HashMap;
import java.util.Map;

public class URIPattern extends URILike<LayerPattern> {
  {
    type = "Pattern";
  }

  public URIPattern(Map<String, Object> pattern) {
    String file = (String) pattern.getOrDefault("file", "**");
    layers.add(new LayerPattern(file, Language.OTHER));
    while (pattern != null) {
      String identifier = (String) pattern.getOrDefault("identifier", "**");
      Language lang = Language.valueOfLabel(pattern.getOrDefault("lang", "*").toString().toLowerCase());
      layers.add(new LayerPattern(identifier, lang));
      pattern = (Map<String, Object>) pattern.get("inline");
    }
  }

  public Language getLang() {
    if (layers.size() < 1) return Language.ANY;
    return layers.get(1).getLanguage();
  }

  /**
   * Match uri, return null if not matched, or a match with captured groups
   *
   * @param uri uri
   * @return captures
   */
  public Capture match(URI uri) {
    return match(uri, new Capture());
  }

  /**
   * Match uri, return null if not matched, or a match with captured groups
   *
   * @param uri uri
   * @return captures
   */
  public Capture match(URI uri, Capture variables) {
    // Part 1: match protocol
    if (!isRef && uri.isRef) return null;

    // Part 2: match depth
    int depth = layers.size();
    if (uri.getLayerCount() < depth) return null;

    // Part 3: match every layers
    Capture result = new Capture();
    for (int i = 0; i < depth; ++i) {
      Capture capture = layers.get(i).match(uri.getLayer(i), variables);
      if (capture == null) return null;
      result.putAll(capture);
    }

    // return captures
    return result;
  }
}
