package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URILike;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class URIPattern extends URILike<LayerPattern> {
  public final Set<String> symbols = new HashSet<>();
  public final Set<String> anchors = new HashSet<>();

  {
    type = "Pattern";
  }

  public URIPattern(boolean isRef, Map<String, Object> pattern) {
    this.isRef = isRef;
    String file = (String) pattern.getOrDefault("file", "**");
    addLayer(file, Language.OTHER);
    while (pattern != null) {
      String identifier = (String) pattern.getOrDefault("identifier", "**");
      Language lang = Language.valueOfLabel(pattern.getOrDefault("lang", "*").toString().toLowerCase());
      addLayer(identifier, lang);
      pattern = (Map<String, Object>) pattern.get("inline");
    }
  }

  private void addLayer(String identifier, Language language) {
    LayerPattern layer = new LayerPattern(identifier, language);
    layers.add(layer);
    symbols.addAll(layer.symbols);
    anchors.addAll(layer.anchors);
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
