package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.gen.html.model.DialectNode;
import edu.pku.code2graph.gen.html.model.NodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandardDialectParser {
  private Stack<Integer> startPoint = new Stack<>();
  private Stack<DialectNode> childrenPool = new Stack<>();
  private List<String> metaChar = Arrays.asList("${", "*{", "#{", "@{", "~{");

  public DialectNode parseTree(String source) {
    clearAll();

    int sourceLen = source.length();
    for (int i = 0; i < sourceLen; i++) {
      if (source.charAt(i) == '(' || source.charAt(i) == ')') continue;
      if (i + 2 <= sourceLen && metaChar.contains(source.substring(i, i + 2))) {
        startPoint.push(i);
        i++;
      } else if (source.charAt(i) == '{') {
        startPoint.push(i);
      } else if (source.charAt(i) == '}') {
        if (startPoint.empty()) {
          continue;
        }
        int s = startPoint.pop(), e = i;

        if (source.charAt(s) == '{') continue;

        String snippet = source.substring(s, e + 1);
        String name = snippet.trim();
        DialectNode node = new DialectNode(name, snippet, NodeType.VAR);

        node.setStartIdx(s);
        node.setEndIdx(e);

        while (!childrenPool.isEmpty()) {
          DialectNode child = childrenPool.peek();
          if (child.getStartIdx() >= s && child.getEndIdx() <= e) {
            childrenPool.pop();
            node.getChildren().add(child);
            child.setParent(node);
          } else {
            break;
          }
        }

        childrenPool.push(node);
      }
    }
    if (childrenPool.isEmpty()) {
      return null;
    }
    return childrenPool.pop();
  }

  public List<DialectNode> parseTreeToList(String source) {
    List<DialectNode> nodeList = new ArrayList<>();
    Pattern pattern = Pattern.compile("[$*#@~]\\{[a-zA-Z0-9-_.\\s]+}");
    Matcher matcher = pattern.matcher(source);
    while (matcher.find()) {
      String e = matcher.group();
      DialectNode node = new DialectNode(e, e, NodeType.VAR);
      nodeList.add(node);
    }
    return nodeList;
  }

  private void clearAll() {
    startPoint.clear();
    childrenPool.clear();
  }
}
