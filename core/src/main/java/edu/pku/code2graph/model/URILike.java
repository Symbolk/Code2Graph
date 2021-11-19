package edu.pku.code2graph.model;

import java.util.ArrayList;
import java.util.List;

public abstract class URILike<T extends Layer> {
    public boolean isRef;
    protected String type;
    protected List<T> layers = new ArrayList<>();

    public int getLayerCount() {
        return layers.size();
    }

    public Layer getLayer(int index) {
        return layers.get(index);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(type);
        output.append(" <");
        output.append(isRef ? "use" : "def");
        output.append(":");
        for (Layer layer : layers) {
            output.append("//").append(layer.getIdentifier());
        }
        return output.append(">").toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(URILike uri) {
        return toString().equals(uri.toString());
    }
}
