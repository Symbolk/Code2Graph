package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URIBase;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class URIPattern extends URIBase<LayerPattern> {
  public final Set<String> symbols = new HashSet<>();
  public final Set<String> anchors = new HashSet<>();

  public URIPattern() {}

  public URIPattern(boolean isRef, String file) {
    this.isRef = isRef;
    addLayer(file, Language.FILE);
  }

  public URIPattern(boolean isRef, Map<String, Object> pattern) {
    this.isRef = isRef;
    String file = (String) pattern.getOrDefault("file", "**");
    addLayer(file, Language.FILE);
    do {
      String identifier = (String) pattern.getOrDefault("identifier", "**");
      Language lang = Language.valueOfLabel(pattern.getOrDefault("lang", "*").toString().toLowerCase());
      if (pattern.containsKey("inline") || !identifier.equals("**")) {
        LayerPattern layer = addLayer(identifier, lang);
        for (String key : pattern.keySet()) {
          if (key.equals("identifier") || key.equals("lang") || key.equals("inline") || key.equals("file")) continue;
          layer.put(key, pattern.get(key).toString());
        }
      }
      pattern = (Map<String, Object>) pattern.get("inline");
    } while (pattern != null);
  }

  public LayerPattern addLayer(String identifier, Language language) {
    LayerPattern layer = new LayerPattern(identifier, language, this);
    layers.add(layer);
    return layer;
  }

  public Capture match(URI uri) {
    return match(uri, new Capture(), true);
  }

  public Capture match(URI uri, Capture variables) {
    return match(uri, variables, false);
  }

  /**
   * Match uri, return null if not matched, or a match with captured groups
   *
   * @param uri uri
   * @return captures
   */
  public Capture match(URI uri, Capture variables, boolean ignoreAnchors) {
    // Part 1: match protocol
    if (!isRef && uri.isRef) return null;

    // Part 2: match depth
    int depth = layers.size();
    if (uri.getLayerCount() < depth) return null;

    // Part 3: match every layers
    Capture result = new Capture();
    for (int i = 0; i < depth; ++i) {
      LayerPattern layer = layers.get(i);
      if (!layer.match(uri.getLayer(i), variables, result, ignoreAnchors)) return null;
    }

    return result;
  }

  public URI refactor(URI target, Capture input, Capture output) {
    URI uri = new URI();
    uri.isRef = target.isRef;
    for (int index = 0; index < layers.size(); ++index) {
      Layer result = layers.get(index).refactor(target.getLayer(index), input, output);
      if (result == null) return null;
      uri.addLayer(result);
    }
    return uri;
  }
}
