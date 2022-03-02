package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Layer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LayerPattern extends Layer {
  private final boolean pass;
  private int offset = 0;
  private String source;
  private String leading;
  private final List<Token> tokens = new ArrayList<>();

  public final List<String> anchors = new ArrayList<>();
  public final List<Token> symbols = new ArrayList<>();

  public LayerPattern(String identifier, Language language) {
    super(identifier, language);

    pass = identifier.equals("**");
    if (pass) return;

    source =
        ("**/" + identifier)
            .replaceAll("\\\\", "\\\\\\\\")
            .replaceAll("\\.", "\\\\.")
            .replaceAll("\\^", "\\\\^")
            .replaceAll("\\$", "\\\\\\$")
            .replaceAll("\\+", "\\\\+")
            .replaceAll("\\*\\*/", "(?:.+/)?")
            .replaceAll("\\*", "\\\\w+")
            .replaceAll("\\{", "\\\\{");

    Matcher matcher = VARIABLE.matcher(source);
    while (matcher.find()) {
      Token token = new Token(matcher);
      if (token.isGreedy) {
        leading = token.name;
        source = source.substring(8);
        offset = 8;
      }
      if (token.isAnchor) {
        anchors.add(token.name);
        tokens.add(token);
      } else {
        symbols.add(token);
      }
    }
  }

  public Capture match(Layer layer, Capture variables) {
    if (pass) return new Capture();

    for (String key : attributes.keySet()) {
      String value = layer.getAttribute(key);
      if (value == null || !value.equals(attributes.get(key))) {
        return null;
      }
    }

    String source = this.source;
    for (int index = tokens.size(); index > 0; --index) {
      Token anchor = tokens.get(index - 1);
      String value = variables.getOrDefault(anchor.name, "[\\w-.]+");
      source = anchor.replace(source, value, offset);
    }
    String[] segments = VARIABLE.split(source, -1);
    if (segments[0].equals("")) {
      source = "(.+)?" + String.join("([\\w-.]+)", segments).substring(9);
    } else {
      source = String.join("([\\w-.]+)", segments);
    }

    String target = layer
        .getIdentifier()
        .replace("\\/", "__slash__");

    Pattern regexp = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
    Matcher matcher = regexp.matcher(target);
    if (!matcher.matches()) return null;

    Capture captures = new Capture();
    int count = matcher.groupCount();
    for (int i = 1; i <= count; ++i) {
      String value = matcher.group(i)
          .replace("__slash__", "/")
          .replace("-", "")
          .replace("_", "")
          .toLowerCase();
      Token token = symbols.get(i - 1);
      if (token.modifier.equals("dot")) {
        value = value.replace(".", "/");
      }
      captures.put(token.name, value);
    }
    if (leading != null) captures.greedy.add(leading);
    return captures;
  }

  public static final Pattern VARIABLE = Pattern.compile("\\(&?(\\w+)(?::(\\w+))?((\\\\\\.){3})?\\)");
}
