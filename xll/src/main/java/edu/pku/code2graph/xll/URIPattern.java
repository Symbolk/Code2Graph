package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.URI;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URIPattern extends URI {
  private final List<Token> anchors = new ArrayList<>();
  private final List<List<String>> layerTokens = new ArrayList<>();

  {
    type = "Pattern";
  }

  public URIPattern() {}

  public URIPattern(URIPattern pattern) {
    this.isRef = pattern.isRef;
    this.lang = pattern.lang;
    this.file = pattern.file;
    this.identifier = pattern.identifier;
    if (pattern.inline != null) {
      this.inline = new URIPattern((URIPattern) pattern.inline);
    }
    this.getLayers();
  }

  public URIPattern(Map<String, Object> pattern) {
    this.lang = Language.valueOfLabel(pattern.getOrDefault("lang", "*").toString().toLowerCase());
    this.file = (String) pattern.getOrDefault("file", "");
    this.identifier = (String) pattern.getOrDefault("identifier", "");
    if (pattern.get("inline") != null) {
      this.inline = new URIPattern((Map<String, Object>) pattern.get("inline"));
    }
    this.getLayers();
  }

  @Override
  protected void parseLayer(String source) {
    List<String> layer = new ArrayList<>();
    if (source == null) {
      source = "**";
    } else {
      Matcher matcher = Token.anchor.matcher(source);
      while (matcher.find()) {
        Token token = new Token(matcher, layers.size());
        anchors.add(token);
        layer.add(token.name);
      }
    }
    layerTokens.add(layer);
    super.parseLayer(source);
  }

  private Map<String, String> matchLayer(int level, String target, Map<String, String> variables) {
    String source = layers.get(level);
    if (source.equals("**")) return new HashMap<>();
    List<String> names = layerTokens.get(level);
    source = "**/" + source;
    source =
        source
            .replaceAll("\\\\", "\\\\\\\\")
            .replaceAll("\\.", "\\\\.")
            .replaceAll("\\^", "\\\\^")
            .replaceAll("\\$", "\\\\\\$")
            .replaceAll("\\+", "\\\\+")
            .replaceAll("\\*\\*/", "(?:.+/)?")
            .replaceAll("\\*", "\\\\w+")
            .replaceAll("\\{", "\\\\{");
    String[] segments = Token.variable.split(source, -1);
    source = String.join("(\\w+)", segments);
    Pattern regexp = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
    target =
        target
            .replace("-", "")
            .replace("_", "")
            .toLowerCase();
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
   *
   * @param uri uri
   * @return captures
   */
  public Capture match(URI uri, Map<String, String> variables) {
    // Part 1: match depth
    int depth = getLayers().size();
    if (uri.getLayers().size() < depth) return null;

    // Part 2: match protocol
    if (!isRef && uri.isRef) return null;

    // Part 3: match every layers
    Capture capture = new Capture();
    for (int i = 0; i < depth; ++i) {
      Map<String, String> cap = matchLayer(i, uri.getLayers().get(i), variables);
      if (cap == null) return null;
      for (String name : cap.keySet()) {
        capture.put(name, cap.get(name));
      }
    }

    // return captures
    return capture;
  }

  /**
   * apply captures
   *
   * @param captures matched result
   * @return new pattern
   */
  public URIPattern applyCaptures(Map<String, String> captures) {
    URIPattern pattern = new URIPattern(this);
    for (String name : captures.keySet()) {
      Token token = anchors.get(name);
      if (token != null) {
        String source = pattern.getLayers().get(token.level);
        pattern.getLayers().set(token.level, token.replace(source, captures.get(name)));
        pattern.anchors.remove(name);
        pattern.layerTokens.get(token.level).remove(name);
      }
    }
    return pattern;
  }

  private static final class Token {
    public static final Pattern variable = Pattern.compile("\\((\\w+)(?::(\\w+))?\\)");
    public static final Pattern anchor = Pattern.compile("\\(&(\\w+)(?::(\\w+))?\\)");

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
