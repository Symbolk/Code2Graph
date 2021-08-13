package edu.pku.code2graph.model;

import java.util.ArrayList;
import java.util.List;

/** Unified Resource Identifier for code elements */
public class URI {
  protected Protocol protocol;
  protected String lang;
  protected String file;
  protected String identifier;
  protected URI inline;
  protected List<String> layers;

  public URI() {
    this.protocol = Protocol.UNKNOWN;
    this.lang = "";
    this.file = "";
    this.identifier = "";
    this.layers = new ArrayList<>();
  }

  protected void parseLayer(String source) {
    layers.add(source);
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public String getLang() {
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
    URI p = this;
    parseLayer(file);
    do {
      parseLayer(p.identifier);
    } while (p.inline != null);
    return layers;
  }

  public URI getInline() {
    return inline;
  }

  public void setProtocol(Protocol protocol) {
    this.protocol = protocol;
  }

  public void setLang(String lang) {
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
    StringBuilder output = new StringBuilder(protocol.toString() + "://" + file);
    for (String layer: getLayers()) {
      output.append("/#").append(layer);
    }
    return output.toString();
  }
}
