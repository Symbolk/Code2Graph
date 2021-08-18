package edu.pku.code2graph.model;

import java.util.ArrayList;
import java.util.List;

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

  public URI(String source) {
    String[] result = source.split("//");
    this.protocol = Protocol.valueOf(result[0].substring(0, result[0].length() - 1).toUpperCase());
    this.lang = Language.ANY;
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
}
