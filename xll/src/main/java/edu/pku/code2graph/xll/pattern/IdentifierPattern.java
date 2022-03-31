package edu.pku.code2graph.xll.pattern;

import edu.pku.code2graph.xll.Capture;
import edu.pku.code2graph.xll.Fragment;
import edu.pku.code2graph.xll.URIPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdentifierPattern extends AttributePattern {
  public static final Pattern VARIABLE = Pattern.compile("\\(&?(\\w+)(?::(\\w+))?((\\\\\\.){3})?\\)");

  private final boolean pass;
  private int offset = 0;
  private String source;
  private final List<Token> anchors = new ArrayList<>();
  private final List<Token> symbols = new ArrayList<>();

  public IdentifierPattern(String name, String identifier, URIPattern root) {
    super(name, root, 100);
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
            .replaceAll("/\\*\\*", "(?:/.+)?")
            .replaceAll("\\*", "\\\\w+")
            .replaceAll("\\{", "\\\\{");

    Matcher matcher = VARIABLE.matcher(source);
    while (matcher.find()) {
      Token token = new Token(matcher);
      if (token.modifier.equals("slash")) {
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

  private String prepare(Capture input) {
    // step 1: replace anchors
    String source = this.source;
    for (int index = anchors.size(); index > 0; --index) {
      Token anchor = anchors.get(index - 1);
      String value = input.containsKey(anchor.name)
          ? input.get(anchor.name).text
          : anchor.modifier.equals("slash")
          ? "[\\w-./]+"
          : "[\\w-.]+";
      source = anchor.replace(source, value, offset);
    }

    // step 2: replace symbols
    String[] segments = VARIABLE.split(source, -1);
    if (segments[0].equals("")) {
      source = "(.+)?" + String.join("([\\w-.]+)", segments).substring(9);
    } else {
      source = String.join("([\\w-.]+)", segments);
    }

    return source;
  }

  public boolean match(String target, Capture input, Capture result) {
    if (pass) return true;

    String source = prepare(input);
    target = target.replace("\\/", "__slash__");

    Pattern regexp = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
    Matcher matcher = regexp.matcher(target);
    if (!matcher.matches()) return false;

    int count = matcher.groupCount();
    for (int i = 1; i <= count; ++i) {
      String value = matcher.group(i)
          .replace("__slash__", "/");
      Token token = symbols.get(i - 1);
      Fragment fragment = new Fragment(value, token.modifier);
      if (fragment.plain.isEmpty()) return false;
      if (!result.accept(token.name, fragment)) return false;
    }

    return true;
  }

  public String hydrate(String target, Capture input, Capture output) {
    if (pass) return target;

    String source = prepare(input);
    target = target.replace("\\/", "__slash__");

    Pattern regexp = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
    Matcher matcher = regexp.matcher(target);

    if (!matcher.matches()) {
      System.out.println(target);
      System.out.println(source);
    }

    int count = matcher.groupCount();
    int lastIndex = 0;
    StringBuilder builder = new StringBuilder();
    for (int i = 1; i <= count; ++i) {
      Token token = symbols.get(i - 1);
      builder.append(target.substring(lastIndex, matcher.start(i)).replace("__slash__", "/"));
      builder.append(output.get(token.name).text);
      lastIndex = matcher.end(i);
    }

    return builder.append(target.substring(lastIndex).replace("__slash__", "/")).toString();
  }
}
