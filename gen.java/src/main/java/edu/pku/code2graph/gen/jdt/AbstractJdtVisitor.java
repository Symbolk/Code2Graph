package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Range;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.GraphUtil;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.pku.code2graph.model.TypeSet.type;

public abstract class AbstractJdtVisitor extends ASTVisitor {
  protected Logger logger = LoggerFactory.getLogger(AbstractJdtVisitor.class);

  // final constructed graph instance
  protected Graph<Node, Edge> graph = GraphUtil.getGraph();

  // temporarily keep the current cu and file path
  protected CompilationUnit cu;
  protected String filePath;

  // TODO index nodes by qualified name as Trie to speed up matching, or just use hash?
  // TODO include external type declaration or not?
  // intermediate cache to build nodes and edges
  // basic assumption: qualified name is unique in one project
  protected Map<String, Node> defPool = new HashMap<>(); // should be ElementNode in theory, but use node to avoid casting when adding edges
  protected List<Triple<Node, Type, String>> usePool = new ArrayList<>();

  public AbstractJdtVisitor() {
    super(true);
  }

  /** Build edges with cached data pool */
  public void buildEdges() {
    for (Triple<Node, Type, String> entry : usePool) {
      Node src = entry.getFirst();
      Node tgt = defPool.get(entry.getThird());
      if (tgt != null) {
        graph.addEdge(src, tgt, new Edge(GraphUtil.eid(), entry.getSecond()));
      }
    }
  }

  /**
   * Just a getter for the graph at present
   *
   * @return
   */
  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  protected static Type nodeAsSymbol(ASTNode node) {
    return nodeAsSymbol(node.getNodeType());
  }

  protected static Type nodeAsSymbol(int id) {
    return type(ASTNode.nodeClassForType(id).getSimpleName());
  }

  public void setCu(CompilationUnit cu) {
    this.cu = cu;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
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
