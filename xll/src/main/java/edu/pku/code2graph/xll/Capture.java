package edu.pku.code2graph.xll;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public final class Capture extends TreeMap<String, String> {
    public Capture project(Collection<String> collection) {
        Capture capture = new Capture();
        for (String key : collection) {
            if (containsKey(key)) {
                capture.put(key, get(key));
            }
        }
        return capture;
    }

    public int hashCode() {
        String hash = "";
        for (Map.Entry entry : this.entrySet()) {
            hash += entry.getKey() + ":" + entry.getValue() + ";";
        }
        return hash.hashCode();
    }

    public boolean equals(Capture capture) {
        return hashCode() == capture.hashCode();
    }
}
