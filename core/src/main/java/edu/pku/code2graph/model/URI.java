package edu.pku.code2graph.model;

/** Unified Resource Identifier for code elements */
public class URI {
  private boolean def;
  private String lang;
  private String file;
  private String identifier;
  private URI inline;
  // concrete URI (default) or XLL pattern
  public boolean isPattern = false;

  public URI() {
    this.def = false;
    this.lang = "";
    this.file = "";
    this.identifier = "";
    this.inline = new URI();
  }

  public boolean isDef() {
    return def;
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

  public void setDef(boolean def) {
    this.def = def;
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
        + "def="
        + def
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
