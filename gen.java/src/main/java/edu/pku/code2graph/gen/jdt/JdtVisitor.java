package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.model.OperationNode;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class JdtVisitor extends AbstractJdtVisitor {
  @Override
  public void preVisit(ASTNode n) {}

  public boolean visit(MethodDeclaration md) {

    return true;
  }

  public boolean visit(MethodInvocation mi) {
    // create the current node
    OperationNode n = new OperationNode();
    // create or find the property node
  
    // add nodes to the graph

    // link properties with edges

    return false;
  }
}
