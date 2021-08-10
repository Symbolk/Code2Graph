package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.pku.code2graph.model.TypeSet.type;

public class AbstractHandler {
  protected Logger logger = LoggerFactory.getLogger(DocumentHandler.class);

  protected Graph<Node, Edge> graph = GraphUtil.getGraph();

  List<String> pre = Arrays.asList("\\@", "\\*", "\\(", "\\)", "\\/", "\\[", "\\]", "\\:");

  // temporarily save the current file path here
  protected String filePath;

  protected Stack<ElementNode> stk = new Stack<>();
  public static final Type CHILD = type("child");
  public static final Type ATTR = type("attribute");

  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getIdentifier(String self) {
    String idtf = "";
    for (ElementNode node : stk) {
      idtf = idtf + checkInvalidCh(node.getName()) + "/";
    }
    idtf = idtf + self;
    return idtf;
  }

  public String checkInvalidCh(String name) {
    for (String ch : pre) {
      Pattern p = Pattern.compile(ch);
      Matcher m = p.matcher(name);
      name = m.replaceAll("\\" + ch);
    }
    return name;
  }
}
