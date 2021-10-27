package edu.pku.code2graph.xll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Step {
    public final String rule;
    public final List<String> after;
    public final Map<String, String> with;

    public Step(Map<String, Object> step) {
        // rule
        this.rule = (String) step.get("rule");

        // after
        Object after = step.get("after");
        if (after instanceof String) {
            this.after = new ArrayList<>();
            this.after.add((String) after);
        } else if (after == null) {
            this.after = new ArrayList<>();
        } else {
            this.after = (List<String>) after;
        }

        // with
        this.with = (Map<String, String>) step.getOrDefault("with", new HashMap<>());
    }
}
