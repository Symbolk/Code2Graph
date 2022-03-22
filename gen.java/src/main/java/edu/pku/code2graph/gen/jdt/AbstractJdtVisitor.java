package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractJdtVisitor extends ASTVisitor {
  protected Logger logger = LoggerFactory.getLogger(AbstractJdtVisitor.class);

  // final constructed graph instance
  protected Graph<Node, Edge> graph = GraphUtil.getGraph();

  // temporarily keep the current cu and file path
  protected CompilationUnit cu;
  protected String filePath;
  protected String uriFilePath;
  protected String identifier;
  protected String scope = "";
  private final Stack<String> stack = new Stack<>();

  public AbstractJdtVisitor() {
    super(true);
  }

  protected ElementNode createElementNode(
      Type type, String snippet, String name, String qname, String identifier) {
    URI uri = new URI(false, uriFilePath);
    uri.addLayer(identifier.replace(".", "/").replaceAll("\\(.+?\\)", ""), Language.JAVA);
    ElementNode node =
        new ElementNode(GraphUtil.nid(), Language.JAVA, type, snippet, name, qname, uri);
    graph.addVertex(node);
    this.identifier = identifier;
    GraphUtil.addNode(node);
    return node;
  }

  protected void pushScope(String prefix) {
    String oldScope = scope;
    scope += prefix + "/";
    stack.push(oldScope);
  }

  protected void popScope() {
    scope = stack.pop();
  }

  protected URI createIdentifier(String identifier) {
    return createIdentifier(identifier, true);
  }

  protected URI createIdentifier(String identifier, boolean isRef) {
    if (identifier == null && scope.length() > 0) {
      identifier = scope.substring(0, scope.length() - 1);
    } else {
      identifier = scope + identifier;
    }
    URI uri = new URI(isRef, uriFilePath);
    uri.addLayer(identifier, Language.JAVA);
    return uri;
  }

  /**
   * Just a getter for the graph at present
   *
   * @return
   */
  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public void setCu(CompilationUnit cu) {
    this.cu = cu;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
    this.uriFilePath = FileUtil.getRelativePath(filePath);
  }

  protected Range computeRange(ASTNode node) {
    int startPosition = node.getStartPosition();
    int endPosition = startPosition + node.getLength() - 1;
    return new Range(
        cu.getLineNumber(startPosition),
        cu.getLineNumber(endPosition),
        cu.getColumnNumber(startPosition),
        cu.getColumnNumber(endPosition));
  }

  protected Range computeRange(List<ASTNode> nodes) {
    if (nodes.isEmpty()) {
      return new Range(-1, -1);
    }
    int startPosition = nodes.get(0).getStartPosition();
    ASTNode lastNode = nodes.get(nodes.size() - 1);
    int endPosition = lastNode.getStartPosition() + lastNode.getLength() - 1;
    return new Range(
        cu.getLineNumber(startPosition),
        cu.getLineNumber(endPosition),
        cu.getColumnNumber(startPosition),
        cu.getColumnNumber(endPosition));
  }
}
