package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.HashMap;
import java.util.Map;

import static edu.pku.code2graph.model.TypeSet.type;

public abstract class AbstractJdtVisitor extends ASTVisitor {
  // final constructed graph instance
  protected Graph<Node, Edge> graph = initGraph();

  // TODO index nodes by qualified name as Trie to speed up matching
  // TODO include external type declaration or not?
  // intermediate cache to build nodes and edges
  // basic assumption: qualified name is unique in one project
  protected Map<String, Node> defPool = new HashMap<>();
  protected Map<Node, Pair<Type, String>> usePool = new HashMap<>();

  public AbstractJdtVisitor() {
    super(true);
  }

  /**
   * Initialize an empty Graph
   *
   * @return
   */
  public Graph<Node, Edge> initGraph() {
    return GraphTypeBuilder.<Node, Edge>directed()
        .allowingMultipleEdges(true) // allow multiple edges with different types
        .allowingSelfLoops(true) // allow recursion
        .edgeClass(Edge.class)
        .weighted(true)
        .buildGraph();
  }

  /** Build edges with cached data pool */
  public void buildEdges() {
    for (Map.Entry entry : usePool.entrySet()) {
      Node src = (Node) entry.getKey();
      Pair<Type, String> use = (Pair<Type, String>) entry.getValue();
      Node tgt = defPool.get(use.getSecond());
      if (tgt != null) {
        graph.addEdge(src, tgt, new Edge(use.getFirst()));
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
}
