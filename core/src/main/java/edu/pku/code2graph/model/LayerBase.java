package edu.pku.code2graph.model;

import java.util.HashMap;

public abstract class LayerBase extends HashMap<String, String> {
  @Deprecated
  public Language getLanguage() {
    return Language.valueOf(get("language"));
  }

  @Deprecated
  public String getIdentifier() {
    return get("identifier");
  }

  @Deprecated
  public void setLanguage(Language language) {
    put("language", language.toString());
  }

  @Deprecated
  public void setIdentifier(String identifier) {
    put("identifier", identifier);
  }

  @Deprecated
  public void addAttribute(String key, String value) {
    put(key, value);
  }

  static public final String backslash = "%%__BACKSLASH__%%";

  static public String escape(String source) {
    return source
        .replace("\\", "\\\\")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace(",", "\\,");
  }

  static public String unescape(String source) {
    return source
        .replace("\\,", ",")
        .replace("\\]", "]")
        .replace("\\[", "[")
        .replace(backslash, "\\");
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(escape(get("identifier")));
    builder.append("[");
    boolean flag = false;
    for (String key : keySet()) {
      if (key.equals("identifier")) continue;
      if (flag) builder.append(",");
      builder.append(key).append("=").append(escape(get(key)));
      flag = true;
    }
    builder.append("]");
    return builder.toString();
  }
}
