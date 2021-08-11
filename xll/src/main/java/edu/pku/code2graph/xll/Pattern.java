package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.URI;

import java.util.Map;

public class Pattern extends URI {
    /**
     * Compile wildcard layers into regex expressions
     */
    public String compile(Pattern pattern) {
        // for file and identifier layers

        // replace grouping/meta-chars

        // transpile to regex

        return "";
    }

    /**
     * Match uri, return null if not matched, or a match with captured groups
     * @param uri
     * @return
     */
    public Map<String, String> match(URI uri) {
        return null;
    }

    /**
     *
     * @param pattern
     * @param capture
     * @return
     */
    public  Pattern applyCaptures(Pattern pattern, String capture) {
        return null;
    }
}
