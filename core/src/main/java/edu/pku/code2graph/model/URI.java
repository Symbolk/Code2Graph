package edu.pku.code2graph.model;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Unified Resource Identifier for code elements */
public class URI extends URILike {
  {
    type = "Pattern";
  }

  public URI() {
    this(false, "");
  }

  public URI(boolean isRef, String file) {
    this.isRef = isRef;
    this.layers.add(new Layer(file));
  }

  public URI(boolean isRef, Language lang, String file, String identifier) {
    this.isRef = isRef;
    this.layers.add(new Layer(file));
    this.layers.add(new Layer(identifier, lang));
  }

  public URI(boolean isRef, Language lang, String file, String identifier, URI inline) {
    this.isRef = isRef;
    this.layers.add(new Layer(file));
    this.layers.add(new Layer(identifier, lang));
    this.lang = lang;
    this.file = file;
    this.identifier = identifier;
    this.inline = inline;
  }

  public URI(String source) {
    String[] result = source.split("//");
    this.isRef = result[0].substring(0, result[0].length() - 1).equals("use");
    this.lang = Language.valueOfLabel(FilenameUtils.getExtension(result[1]).toLowerCase());
    this.file = result[1];
    this.identifier = result[2];
    URI p = this;
    for (int i = 3; i < result.length; ++i) {
      p.inline = new URI();
      p = p.inline;
      p.identifier = result[i];
    }
  }

  public void addLayer(String identifier) {
    this.addLayer(identifier, Language.ANY);
  }

  public void addLayer(String identifier, Language language) {
    this.layers.add(new Layer(identifier, language));
  }

  protected void parseLayer(String source) {
    layers.add(source);
  }

  public Language getLang() {
    return lang;
  }

  public String getFile() {
    return file;
  }

  public String getIdentifier() {
    return identifier;
  }

  public List<String> getLayers() {
    return getLayers(false);
  }

  public List<String> getLayers(boolean forced) {
    if (layers != null && !forced) return layers;
    layers = new ArrayList<>();
    URI p = this;
    parseLayer(file);
    do {
      parseLayer(p.identifier);
      p = p.inline;
    } while (p != null);
    return layers;
  }

  public URI getInline() {
    return inline;
  }

  public String getSymbol() {
    String[] split;
    if (inline == null) split = identifier.split("//");
    else split = inline.identifier.split("//");
    split = split[split.length - 1].split("/");
    return split[split.length - 1];
  }

  public int getIdentifierSegmentCount() {
    return identifier.replaceAll("\\\\/", "").split("/").length;
  }

  public void setLang(Language lang) {
    this.lang = lang;
  }

  public void setFile(String file) {
    this.file = file;
    this.getLayers(true);
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
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
}
