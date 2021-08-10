package edu.pku.code2graph.model;

/** Unified Resource Identifier for code elements */
public class URI {
  private Protocol protocol;
  private String lang;
  private String file;
  private String identifier;
  private URI inline;

  public URI() {
    this.protocol = Protocol.UNKNOWN;
    this.lang = "";
    this.file = "";
    this.identifier = "";
    this.inline = new URI();
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
    return "Rule{"
        + "protocol="
        + protocol.toString()
        + ", lang='"
        + lang
        + '\''
        + ", file='"
        + file
        + '\''
        + ", identifier='"
        + identifier
        + '\''
        + '}';
  }
}
