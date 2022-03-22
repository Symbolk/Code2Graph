package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdentifierPattern extends AttributePattern {
  public static final Pattern VARIABLE = Pattern.compile("\\(&?(\\w+)(?::(\\w+))?((\\\\\\.){3})?\\)");

  private final boolean pass;
  private int offset = 0;
  private String source;
  private String leading;
  private final List<Token> anchors = new ArrayList<>();
  private final List<Token> symbols = new ArrayList<>();

  public IdentifierPattern(String identifier, URIPattern root) {
    super(root);
    pass = identifier.equals("**");
    if (pass) return;

    source =
        ("**/" + identifier)
            .replaceAll("\\\\/", "__slash__")
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
        root.anchors.add(token.name);
        anchors.add(token);
      } else {
        root.symbols.add(token.name);
        symbols.add(token);
      }
    }
  }

  public Capture match(String target, Capture variables) {
    if (pass) return new Capture();

    String source = this.source;
    for (int index = anchors.size(); index > 0; --index) {
      Token anchor = anchors.get(index - 1);
      String value = variables.containsKey(anchor.name)
        ? variables.get(anchor.name).text
        : "[\\w-.]+";
      source = anchor.replace(source, value, offset);
    }
    String[] segments = VARIABLE.split(source, -1);
    if (segments[0].equals("")) {
      source = "(.+)?" + String.join("([\\w-.]+)", segments).substring(9);
    } else {
      source = String.join("([\\w-.]+)", segments);
    }

    target = target
        .replace("-", "")
        .replace("_", "")
        .replace("\\/", "__slash__")
        .toLowerCase();

    Pattern regexp = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
    Matcher matcher = regexp.matcher(target);
    if (!matcher.matches()) return null;

    Capture captures = new Capture();
    int count = matcher.groupCount();
    for (int i = 1; i <= count; ++i) {
      String value = matcher.group(i)
          .replace("__slash__", "/");
      Token token = symbols.get(i - 1);
      if (token.modifier.equals("dot")) {
        value = value.replace(".", "/");
      }
      captures.put(token.name, value);
    }
    if (leading != null) captures.greedy.add(leading);
    return captures;
  }
}
