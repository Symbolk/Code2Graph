package edu.pku.code2graph.model;

import java.util.HashMap;

public class Layer extends HashMap<String, String> {
    public Layer(String identifier, Language language) {
        super();
        put("identifier", identifier);
        put("language", language.toString());
    }

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

    @Deprecated
    public void addAttribute(String key, String value) {
        put(key, value);
    }

    @Deprecated
    public String getAttribute(String key) {
        return get(key);
    }

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

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object obj) {
        return toString().equals(obj.toString());
    }
}
