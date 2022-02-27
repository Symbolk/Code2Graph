package edu.pku.code2graph.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Layer implements Serializable {
    protected Language language;
    protected String identifier;
    protected Map<String, String> attributes;

    public Layer(String identifier, Language language) {
        this.language = language;
        this.identifier = identifier;
        this.attributes = new HashMap<>();
    }

    public Language getLanguage() {
        return language;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(identifier);
        builder.append("[");
        boolean flag = false;
        for (String key : attributes.keySet()) {
            if (flag) builder.append(",");
            builder.append(key).append("=").append(getAttribute(key));
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
