package edu.pku.code2graph.model;

import java.io.Serializable;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Unified Resource Identifier for code elements */
public final class URI extends URIBase<Layer> implements Serializable {
  public URI() {}

  public URI(boolean isRef, String file) {
    this.isRef = isRef;
    this.addLayer(file, Language.FILE);
  }

  public URI(String source) {
    source = source.replace("\\\\", Layer.backslash);
    String[] result = source.split("(?<!\\\\)//");
    this.isRef = result[0].substring(0, result[0].length() - 1).equals("use");
    for (int i = 1; i < result.length; ++i) {
      layers.add(new Layer(result[i]));
    }
  }

  public Layer addLayer(String identifier, Language language) {
    Layer layer = new Layer(identifier, language);
    layers.add(layer);
    return layer;
  }

  // hack code

  @Deprecated
  public Language getLang() {
    if (layers.size() < 1) return Language.ANY;
    return layers.get(1).getLanguage();
  }

  @Deprecated
  public String getFile() {
    return layers.get(0).getIdentifier();
  }

  @Deprecated
  public String getIdentifier() {
    if (layers.size() <= 1) return "";
    return layers.get(1).getIdentifier();
  }

  @Deprecated
  public String getInlineIdentifier() {
    if (layers.size() <= 2) return "";
    return layers.get(2).getIdentifier();
  }

  @Deprecated
  public String getSymbol() {
    String identifier = layers.get(layers.size() - 1).getIdentifier();
    String[] split = identifier.split("/");
    return split[split.length - 1];
  }

  @Deprecated
  public void setIdentifier(String identifier) {
    if (layers.size() < 1) {
      addLayer(identifier);
    } else {
      layers.get(1).setIdentifier(identifier);
    }
  }

  @Deprecated
  public void setInlineIdentifier(String identifier) {
    while (layers.size() <= 2) {
      addLayer("");
    }
    layers.get(2).setIdentifier(identifier);
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return toString().equals(obj.toString());
  }

  private static List<String> pre = Arrays.asList("\\*", "\\(", "\\)", "\\/", "\\[", "\\]");

  public static String checkInvalidCh(String symbol) {
    for (String ch : pre) {
      Pattern p = Pattern.compile(ch);
      Matcher m = p.matcher(symbol);
      symbol = m.replaceAll("\\" + ch);
    }
    return symbol;
  }

  public static String removeEscapeCh(String symbol) {
    for (String ch : pre) {
      Pattern p = Pattern.compile("\\\\" + ch);
      Matcher m = p.matcher(symbol);
      symbol = m.replaceAll(ch);
    }
    return symbol;
  }
}
