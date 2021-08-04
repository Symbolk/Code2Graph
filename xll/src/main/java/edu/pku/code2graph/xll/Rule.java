package edu.pku.code2graph.xll;

/** Wildcard form of URI */
public class Rule {
  private boolean def;
  private String lang;
  private String file;
  private String identifier;
  private Rule inline;

  public Rule() {
    this.def = false;
    this.lang = "";
    this.file = "";
    this.identifier = "";
    this.inline = null;
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

  public Rule getInline() {
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

  public void setInline(Rule inline) {
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
