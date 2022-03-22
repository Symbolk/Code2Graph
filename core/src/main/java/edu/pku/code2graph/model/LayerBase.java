package edu.pku.code2graph.model;

import java.util.HashMap;

public abstract class LayerBase extends HashMap<String, String> {
  public Language getLanguage() {
    return Language.valueOf(get("language"));
  }

  public String getIdentifier() {
    return get("identifier");
  }

  public void setLanguage(Language language) {
    put("language", language.toString());
  }

  public void setIdentifier(String identifier) {
    put("identifier", identifier);
  }

  public void addAttribute(String key, String value) {
    put(key, value);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(get("identifier"));
    builder.append("[");
    boolean flag = false;
    for (String key : keySet()) {
      if (key.equals("identifier")) continue;
      if (flag) builder.append(",");
      builder.append(key).append("=").append(get(key));
      flag = true;
    }
    builder.append("]");
    return builder.toString();
  }
}
