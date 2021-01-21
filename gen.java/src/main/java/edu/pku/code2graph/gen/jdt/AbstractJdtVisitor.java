package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.GraphUtil;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.pku.code2graph.model.TypeSet.type;

public abstract class AbstractJdtVisitor extends ASTVisitor {
  // final constructed graph instance
  protected Graph<Node, Edge> graph = GraphUtil.getGraph();

  // TODO index nodes by qualified name as Trie to speed up matching, or just use hash?
  // TODO include external type declaration or not?
  // intermediate cache to build nodes and edges
  // basic assumption: qualified name is unique in one project
  protected Map<String, Node> defPool = new HashMap<>();
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
}
