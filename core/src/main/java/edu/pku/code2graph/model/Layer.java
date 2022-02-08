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

    public String toString() {
        return language.toString() + "://" + identifier;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object obj) {
        return toString().equals(obj.toString());
    }
}
