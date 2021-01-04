package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.RelationNode;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.GraphUtil;
import org.eclipse.jdt.core.dom.*;
import org.jgrapht.alg.util.Triple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Visitor to create nodes and collect info to create edges
 *
 * <p>1. visit, store and index all concerned nodes
 *
 * <p>2. create nodes with qualified name, and cache a mapping between qname and node
 *
 * <p>3. cache role and target node with triple of <node, role, qname>
 *
 * <p>5. create edges
 */
public class JdtVisitor extends AbstractJdtVisitor {

  @Override
  public void preVisit(ASTNode n) {}

  public boolean visit(TypeDeclaration td) {
    Type type = td.isInterface() ? NodeType.INTERFACE_DECLARATION : NodeType.CLASS_DECLARATION;
    ITypeBinding tdBinding = td.resolveBinding();
    // isFromSource
    String qname = tdBinding.getQualifiedName();
    ElementNode n =
        new ElementNode(
            GraphUtil.popNodeID(graph), type, td.toString(), td.getName().toString(), qname);
    graph.addVertex(n);
    defPool.put(qname, n);

    IVariableBinding[] fdBindings = tdBinding.getDeclaredFields();
    IMethodBinding[] mdBindings = tdBinding.getDeclaredMethods();
    for (IVariableBinding b : fdBindings) {
      usePool.add(Triple.of(n, EdgeType.CHILD, qname + ":" + b.getName()));
    }
    for (IMethodBinding b : mdBindings) {
      if (b.isDefaultConstructor()) {
        continue;
      }
      usePool.add(Triple.of(n, EdgeType.CHILD, getMethodQNameFromBinding(b)));
    }

    return true;
  }

  public boolean visit(FieldDeclaration fd) {
    List<VariableDeclarationFragment> fragments = fd.fragments();
    for (VariableDeclarationFragment fragment : fragments) {
      String name = fragment.getName().getFullyQualifiedName();
      String qname = name;
      IVariableBinding binding = fragment.resolveBinding();
      if (binding != null && binding.getDeclaringClass() != null) {
        qname = binding.getDeclaringClass().getQualifiedName() + ":" + name;
      }
      ElementNode n =
          new ElementNode(
              GraphUtil.popNodeID(graph),
              NodeType.FIELD_DECLARATION,
              fragment.toString(),
              name,
              qname);
      graph.addVertex(n);
      defPool.put(qname, n);

      if (binding != null && binding.getType().isFromSource()) {
        usePool.add(Triple.of(n, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
      }
    }
    return false;
  }

  public boolean visit(VariableDeclarationStatement vd) {
    for (Iterator iter = vd.fragments().iterator(); iter.hasNext(); ) {
      VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
      IVariableBinding binding = fragment.resolveBinding();
      String name = fragment.getName().getFullyQualifiedName();
      String qname = name;
      if (binding != null && binding.getType().isFromSource()) {
        String parentMethodName = getMethodQNameFromBinding(binding.getDeclaringMethod());
        qname = binding.getType().getQualifiedName() + ":" + parentMethodName + ":" + name;
        ElementNode n =
            new ElementNode(
                GraphUtil.popNodeID(graph),
                NodeType.VAR_DECLARATION,
                fragment.toString(),
                name,
                qname);
        graph.addVertex(n);

        usePool.add(Triple.of(n, EdgeType.PARENT, parentMethodName));
        usePool.add(Triple.of(n, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
      }
    }
    return false;
  }

  public boolean visit(MethodDeclaration md) {
    // A binding represents a named entity in the Java language
    // for internal, should never be null
    IMethodBinding mdBinding = md.resolveBinding();
    String name = getMethodQNameFromBinding(mdBinding);
    String qname = name;
    ElementNode n =
        new ElementNode(
            GraphUtil.popNodeID(graph), NodeType.METHOD_DECLARATION, md.toString(), name, qname);
    graph.addVertex(n);
    defPool.put(qname, n);

    // return type
    ITypeBinding tpBinding = mdBinding.getReturnType();
    if (tpBinding != null) {
      usePool.add(Triple.of(n, EdgeType.METHOD_RETURN, tpBinding.getQualifiedName()));
    }

    // para types
    ITypeBinding[] paraBindings = mdBinding.getParameterTypes();
    if (paraBindings != null) {
      for (ITypeBinding p : paraBindings) {
        usePool.add(Triple.of(n, EdgeType.METHOD_PARAMETER, p.getQualifiedName()));
      }
    }

    // TODO: process body here or else where?
    return true;
  }

  public boolean visit(MethodInvocation mi) {
    // get caller
    // get qname of method invocation
    List<Expression> arguments = mi.arguments();

    IMethodBinding mdBinding = mi.resolveMethodBinding();
    // only internal invocation (or consider types, fields and local?)
    if (mdBinding != null) {
      RelationNode n =
          new RelationNode(GraphUtil.popNodeID(graph), NodeType.METHOD_INVOCATION, mi.toString());
      graph.addVertex(n);

      // called method
      usePool.add(Triple.of(n, EdgeType.METHOD_CALLEE, getMethodQNameFromBinding(mdBinding)));
    }

    return false;
  }

  private String getMethodQNameFromBinding(IMethodBinding binding) {
    ITypeBinding[] paraBindings = binding.getParameterTypes();
    List<String> paraTypes = new ArrayList<>();
    for (ITypeBinding b : paraBindings) {
      paraTypes.add(b.getQualifiedName());
    }

    String qname = binding.getName() + "(" + String.join(",", paraTypes).trim() + ")";
    if (binding != null) {
      qname = binding.getDeclaringClass().getQualifiedName() + ":" + qname;
    }
    return qname;
  }
}
