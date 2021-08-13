package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Protocol;
import edu.pku.code2graph.model.URI;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class URIPattern extends URI {
    protected URIPattern inline;
    private final Map<String, Token> tokens = new HashMap<>();
    private final List<List<String>> layerTokens = new ArrayList<>();

    public URIPattern(URIPattern pattern) {
        this.protocol = pattern.protocol;
        this.lang = pattern.lang;
        this.file = pattern.file;
        this.identifier = pattern.identifier;
        this.layers = new ArrayList<>(pattern.layers);
        if (pattern.inline != null) {
            this.inline = new URIPattern(inline);
        }
    }

    public URIPattern(Map<String, Object> pattern) {
        this.protocol = (Protocol) pattern.getOrDefault("protocol", Protocol.UNKNOWN);
        this.lang = (String) pattern.get("lang");
        this.file = (String) pattern.get("file");
        this.identifier = (String) pattern.get("identifier");
        if (pattern.get("inline") != null) {
            this.inline = new URIPattern((Map<String, Object>) pattern.get("inline"));
        }
    }

    @Override
    protected void parseLayer(String source) {
        List<String> layer = new ArrayList<>();
        Matcher matcher = Token.regexp.matcher(source);
        while (matcher.find()) {
            Token token = new Token(matcher, layers.size());
            tokens.put(token.name, token);
            layer.add(token.name);
        }
        layerTokens.add(layer);
        super.parseLayer(source);
    }

    private Map<String, String> matchLayer(int level, String target) {
        String source = layers.get(level);
        List<String> names = layerTokens.get(level);
        String[] segments = Token.regexp.split(source);
        source = String.join("(\\w+)", segments);
        Pattern regexp = Pattern.compile(source);
        Matcher matcher = regexp.matcher(target);
        if (!matcher.matches()) return null;
        Map<String, String> captures = new HashMap<>();
        int count = matcher.groupCount();
        for (int i = 1; i <= count; ++i) {
            captures.put(names.get(i - 1), matcher.group(i));
        }
        return captures;
    }

    /**
     * Match uri, return null if not matched, or a match with captured groups
     * @param uri uri
     * @return captures
     */
    public Map<String, String> match(URI uri) {
        // Part 1: match depth
        int depth = layers.size();
        if (uri.getLayers().size() < depth) return null;

        // Part 2: match protocol
        String label = protocol.toString();
        if (!label.equals("NA") && !label.equals(uri.getProtocol().toString())) return null;

        // Part 3: match language

        // Part 4: match every layers
        Map<String, String> captures = new HashMap<>();
        for (int i = 0; i < depth; ++i) {
            Map<String, String> cap = matchLayer(i, uri.getLayers().get(i));
            if (cap == null) return null;
            for (String name: cap.keySet()) {
                captures.put(name, cap.get(name));
            }
        }

        // return captures
        return captures;
    }

    /**
     * apply captures
     * @param captures matched result
     * @return new pattern
     */
    public URIPattern applyCaptures(Map<String, String> captures) throws CloneNotSupportedException {
        URIPattern pattern = new URIPattern(this);
        for (String name: captures.keySet()) {
            Token token = tokens.get(name);
            if (token != null) {
                String source = pattern.layers.get(token.level);
                pattern.layers.set(token.level, token.replace(source, captures.get(name)));
                pattern.tokens.remove(name);
                pattern.layerTokens.get(token.level).remove(name);
            }
        }
        return pattern;
    }

    private static class Token {
        public static final Pattern regexp = Pattern.compile("\\((\\w+)(?::(\\w+))?\\)");

        public int level;
        public int start;
        public int end;
        public String name;
        public String modifier;

        public Token(Matcher matcher, int level) {
            this.level = level;
            this.start = matcher.start();
            this.end = matcher.end();
            this.name = matcher.group(1);
            this.modifier = matcher.group(2);
        }

        public String replace(String source, String capture) {
            return source.substring(0, start) + capture + source.substring(end);
        }
    }
}