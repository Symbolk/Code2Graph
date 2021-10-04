package edu.pku.code2graph.xll;

import java.util.HashMap;

public final class Capture extends HashMap<String, String> {
    private String hash = "";

    public String put(String key, String value) {
        hash += key + ":" + value + ";";
        return super.put(key, value);
    }

    public int hashCode() {
        return hash.hashCode();
    }

    public boolean equals(Capture capture) {
        return hash.equals(capture.hash);
    }
}
