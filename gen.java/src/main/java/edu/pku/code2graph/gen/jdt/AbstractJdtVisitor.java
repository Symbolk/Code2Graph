package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.io.FilenameUtils;
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

  // TODO index nodes by qualified name as Trie to speed up matching, or just use hash?
  // TODO include external type declaration or not?
  // intermediate cache to build nodes and edges
  // basic assumption: qualified name is unique in one project
  protected Map<String, Node> defPool =
      new HashMap<>(); // should be ElementNode in theory, but use node to avoid casting when adding
  // edges
  protected List<Triple<Node, Type, String>> usePool = new ArrayList<>();

  public AbstractJdtVisitor() {
    super(true);
  }

  protected ElementNode createElementNode(
      Type type, String snippet, String name, String qname, String identifier) {
    URI uri =
        new URI(
            Protocol.DEF,
            Language.JAVA,
            uriFilePath,
            identifier.replace(".", "/").replaceAll("\\(.+?\\)", ""));
    ElementNode node =
        new ElementNode(GraphUtil.nid(), Language.JAVA, type, snippet, name, qname, uri);
    graph.addVertex(node);
    defPool.put(qname, node);
    this.identifier = identifier;
    GraphUtil.addURI(Language.JAVA, uri, node);
    return node;
  }

  /** Build edges with cached data pool */
  public void buildEdges() {
    for (Triple<Node, Type, String> entry : usePool) {
      Node src = entry.getFirst();
      Optional<Node> tgt = findEntityNodeByName(entry.getThird());
      tgt.ifPresent(node -> graph.addEdge(src, node, new Edge(GraphUtil.eid(), entry.getSecond())));
    }
  }

  protected Optional<Node> findEntityNodeByName(String name) {
    if (defPool.containsKey(name)) {
      return Optional.of(defPool.get(name));
    } else {
      // greedily match as simple name
      return defPool.entrySet().stream()
          .filter(e -> e.getKey().endsWith(name))
          .map(Map.Entry::getValue)
          .findFirst();
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
