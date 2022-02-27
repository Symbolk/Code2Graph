package edu.pku.code2graph.model;

import java.util.ArrayList;
import java.util.List;

public abstract class URILike<T extends Layer> {
    public boolean isRef;
    protected List<T> layers = new ArrayList<>();

    public int getLayerCount() {
        return layers.size();
    }

    public T getLayer(int index) {
        return layers.get(index);
    }

    public T addLayer(String identifier) {
        return addLayer(identifier, Language.ANY);
    }

    public abstract T addLayer(String identifier, Language language);

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("<");
        output.append(isRef ? "use" : "def");
        output.append(":");
        for (Layer layer : layers) {
            output.append("//").append(layer.toString());
        }
        return output.append(">").toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object obj) {
        return toString().equals(obj.toString());
    }
}
