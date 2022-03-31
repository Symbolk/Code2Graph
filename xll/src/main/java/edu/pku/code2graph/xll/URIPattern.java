package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
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
}
