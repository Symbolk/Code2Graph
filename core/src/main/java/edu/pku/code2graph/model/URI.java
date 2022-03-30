package edu.pku.code2graph.model;

import org.apache.commons.io.FilenameUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Unified Resource Identifier for code elements */
public final class URI extends URIBase<Layer> implements Serializable {
  public URI() {
    this(false, "");
  }

  public URI(boolean isRef, String file) {
    this.isRef = isRef;
    this.addLayer(file, Language.FILE);
  }

  public URI(String source) {
    String[] result = source.split("//");
    this.isRef = result[0].substring(0, result[0].length() - 1).equals("use");
    addLayerFromSource(result[1], Language.FILE);

    int splitIdx = getAttrIdx(result[1]);
    Language lang =
        Language.valueOfLabel(
            FilenameUtils.getExtension(result[1].substring(0, splitIdx)).toLowerCase());
    if (result.length <= 2) return;
    String identifier = result[2];
    addLayerFromSource(identifier, lang);

    for (int i = 3; i < result.length; ++i) {
      addLayerFromSource(result[i]);
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

  public static String checkInvalidCh(String name) {
    for (String ch : pre) {
      Pattern p = Pattern.compile(ch);
      Matcher m = p.matcher(name);
      name = m.replaceAll("\\" + ch);
    }
    return name;
  }

  private int getAttrIdx(String source) {
    int splitPoint = source.length() - 1;
    int stackCnt = 0;
    for (; splitPoint >= 0; splitPoint--) {
      if (source.charAt(splitPoint) == ']') {
        stackCnt++;
      } else if (source.charAt(splitPoint) == '[') {
        stackCnt--;
        if (stackCnt == 0) {
          break;
        }
      }
    }

    return splitPoint;
  }

  private Layer addLayerFromSource(String source) {
    return addLayerFromSource(source, Language.ANY);
  }

  private Layer addLayerFromSource(String source, Language lang) {
    int splitPoint = getAttrIdx(source);

    Layer layer = addLayer(source.substring(0, splitPoint), lang);

    String dropBracket = source.substring(splitPoint + 1, source.length() - 1);
    List<String> attrs = new ArrayList<>();
    boolean hasMetEqual = false;
    int end = dropBracket.length() - 1;
    for (int i = dropBracket.length() - 1; i >= 0; i--) {
      if (dropBracket.charAt(i) == ',') {
        if (hasMetEqual) {
          attrs.add(dropBracket.substring(i + 1, end + 1));
          end = i - 1;
        }
        hasMetEqual = false;
      }
      if (dropBracket.charAt(i) == '=') {
        hasMetEqual = true;
      }
      if (i == 0) {
        attrs.add(dropBracket.substring(i, end + 1));
      }
    }
    for (String attr : attrs) {
      String[] pair = attr.split("=");
      assert (pair.length == 2);
      layer.put(pair[0], pair[1]);
    }

    return layer;
  }
}
