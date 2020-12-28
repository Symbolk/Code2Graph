package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.model.*;
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
  // A binding represents a named entity in the Java language
  protected Map<String, ElementNode> elementPool = new HashMap<>();
  protected Map<OperationNode, Pair<EdgeType, String>> operationPool = new HashMap<>();

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
