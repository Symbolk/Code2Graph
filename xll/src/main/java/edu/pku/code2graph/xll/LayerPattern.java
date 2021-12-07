package edu.pku.code2graph.xll;

import edu.pku.code2graph.model.Language;
import edu.pku.code2graph.model.Layer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LayerPattern extends Layer {
  private boolean pass;
  private String source;
  private final List<Token> tokens = new ArrayList<>();

  public final List<String> anchors = new ArrayList<>();
  public final List<String> symbols = new ArrayList<>();

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
      if (token.isAnchor) {
        anchors.add(token.name);
        tokens.add(token);
      } else {
        symbols.add(token.name);
      }
    }
  }

  public Capture match(Layer layer, Capture variables) {
    if (pass) return new Capture();

    String source = this.source;
    for (int index = tokens.size(); index > 0; --index) {
      Token anchor = tokens.get(index - 1);
      String value = variables.getOrDefault(anchor.name, "\\w+");
      source = anchor.replace(source, value);
    }
    String[] segments = VARIABLE.split(source, -1);
    source = String.join("(\\w+)", segments);

    String target = layer
        .getIdentifier()
        .replace("-", "")
        .replace("_", "")
        .toLowerCase();

    Pattern regexp = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
    Matcher matcher = regexp.matcher(target);
    if (!matcher.matches()) return null;

    Capture captures = new Capture();
    int count = matcher.groupCount();
    for (int i = 1; i <= count; ++i) {
      captures.put(symbols.get(i - 1), matcher.group(i));
    }
    return captures;
  }

  public static final Pattern VARIABLE = Pattern.compile("\\(&?(\\w+)(?::(\\w+))?\\)");

  private static final class Token {
    private final int start;
    private final int end;

    public final String name;
    public final String modifier;
    public final boolean isAnchor;

    public Token(Matcher matcher) {
      this.start = matcher.start();
      this.end = matcher.end();
      this.name = matcher.group(1);
      this.modifier = matcher.group(2);
      this.isAnchor = matcher.group(0).charAt(1) == '&';
    }

    public String replace(String source, String capture) {
      return source.substring(0, start) + capture + source.substring(end);
    }
  }
}
