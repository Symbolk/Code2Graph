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
}
