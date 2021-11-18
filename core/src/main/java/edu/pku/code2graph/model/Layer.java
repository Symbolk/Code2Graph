package edu.pku.code2graph.model;

public class Layer {
    public final Language language;
    public final String identifier;

    public Layer(String identifier) {
        this(identifier, Language.OTHER);
    }

    public Layer(String identifier, Language language) {
        this.language = language;
        this.identifier = identifier;
    }
}
