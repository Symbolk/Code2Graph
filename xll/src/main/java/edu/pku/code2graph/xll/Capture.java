package edu.pku.code2graph.xll;

import java.util.Map;
import java.util.TreeMap;

public final class Capture extends TreeMap<String, String> {
    private String hash = "";

    public String put(String key, String value) {
        hash = "";
        for (Map.Entry entry : this.entrySet()) {
            hash += entry.getKey() + ":" + entry.getValue() + ";";
        }
        return super.put(key, value);
    }

    public int hashCode() {
        return hash.hashCode();
    }

    public boolean equals(Capture capture) {
        return hash.equals(capture.hash);
    }
}
