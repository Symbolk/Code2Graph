package edu.pku.code2graph.model;

public class Layer {
    protected Language language;
    protected String identifier;

    public Layer(String identifier, Language language) {
        this.language = language;
        this.identifier = identifier;
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
