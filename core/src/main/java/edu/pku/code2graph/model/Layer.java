package edu.pku.code2graph.model;

public class Layer {
    private Language language;
    private String identifier;

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
