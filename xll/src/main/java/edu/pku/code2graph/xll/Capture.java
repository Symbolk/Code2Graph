package edu.pku.code2graph.xll;

import java.util.*;

public final class Capture extends TreeMap<String, String> {
    public Set<String> greedy = new HashSet<>();

    public Capture project(Collection<String> collection) {
        Capture capture = new Capture();
        for (String key : collection) {
            if (containsKey(key)) {
                capture.put(key, get(key));
                if (greedy.contains(key)) {
                    capture.greedy.add(key);
                }
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

    public boolean match(Capture capture) {
        for (Map.Entry<String, String> entry : this.entrySet()) {
            if (greedy.contains(entry.getKey())) {
                if (!entry.getValue().endsWith(capture.get(entry.getKey()))) return false;
            } else {
                if (!entry.getValue().equals(capture.get(entry.getKey()))) return false;
            }
        }
        return true;
    }

    public void merge(Capture capture) {
        putAll(capture);
        greedy.addAll(capture.greedy);
    }

    @Override
    public String toString() {
        String result = super.toString();
        result += greedy.toString();
        return result;
    }

    public Capture clone() {
        Capture result = (Capture) super.clone();
        result.merge(this);
        return result;
    }
}
