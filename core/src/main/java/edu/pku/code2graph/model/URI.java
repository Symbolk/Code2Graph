package edu.pku.code2graph.model;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  }

  public URI(Protocol protocol, String lang, String file, String identifier) {
    this.protocol = protocol;
    this.lang = lang;
    this.file = file;
    this.identifier = identifier;
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

  private static List<String> pre =
      Arrays.asList("\\@", "\\*", "\\(", "\\)", "\\/", "\\[", "\\]", "\\:");

  public static String checkInvalidCh(String name) {
    for (String ch : pre) {
      Pattern p = Pattern.compile(ch);
      Matcher m = p.matcher(name);
      name = m.replaceAll("\\" + ch);
    }
    return name;
  }
}
