package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import static edu.pku.code2graph.model.TypeSet.type;

public abstract class AbstractJdtVisitor extends ASTVisitor {
  // final constructed graph instance
  protected Graph<Node, Edge> graph = initGraph();

  // index nodes by qualified name Trie to speed up matching
  // or simply use hash?

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
