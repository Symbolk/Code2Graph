package edu.pku.code2graph.model;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Unified Resource Identifier for code elements */
public class URI {
  protected Protocol protocol;
  protected Language lang;
  protected String file;
  protected String identifier;
  protected URI inline;
  protected List<String> layers;
  protected String type = "URI";

  public URI() {
    this.protocol = Protocol.ANY;
    this.lang = Language.ANY;
    this.file = "";
    this.identifier = "";
  }

  public URI(Protocol protocol, Language lang, String file, String identifier) {
    this.protocol = protocol;
    this.lang = lang;
    this.file = file;
    this.identifier = identifier;
  }

  public URI(String source) {
    String[] result = source.split("//");
    this.protocol =
        Protocol.valueOfLabel(result[0].substring(0, result[0].length() - 1).toLowerCase());
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

  protected void parseLayer(String source) {
    layers.add(source);
  }

  public Protocol getProtocol() {
    return protocol;
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
    if (layers != null) return layers;
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

  public void setProtocol(Protocol protocol) {
    this.protocol = protocol;
  }

  public void setLang(Language lang) {
    this.lang = lang;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public void setInline(URI inline) {
    this.inline = inline;
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    output.append(type);
    output.append(" <");
    output.append(protocol.toString());
    output.append(":");
    for (String layer : getLayers()) {
      output.append("//").append(layer);
    }
    return output.append(">").toString();
  }

  public boolean equals(URI uri) {
    return toString().equals(uri.toString());
  }

  private static List<String> pre =
      Arrays.asList("\\*", "\\(", "\\)", "\\/", "\\[", "\\]");

  public static String checkInvalidCh(String name) {
    for (String ch : pre) {
      Pattern p = Pattern.compile(ch);
      Matcher m = p.matcher(name);
      name = m.replaceAll("\\" + ch);
    }
    return name;
  }
}
