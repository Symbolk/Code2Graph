package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.jgrapht.Graph;

import static edu.pku.code2graph.model.TypeSet.type;

public class AbstractJdtVisitor extends ASTVisitor {
  private Graph<Node, Edge> graph;

  public AbstractJdtVisitor() {
    super(true);
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
