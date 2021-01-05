package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.RelationNode;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.GraphUtil;
import org.eclipse.jdt.core.dom.*;
import org.jgrapht.alg.util.Triple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

  public boolean visit(PackageDeclaration pd) {
    return true;
  }

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

    ITypeBinding parentType = tdBinding.getSuperclass();
    if (parentType != null && parentType.isFromSource()) {
      usePool.add(Triple.of(n, EdgeType.PARENT_CLASS, parentType.getQualifiedName()));
    }
    ITypeBinding[] interfaces = tdBinding.getInterfaces();
    if (interfaces != null && interfaces.length > 0) {
      for (int i = 0; i < interfaces.length; ++i) {
        if (interfaces[i].isFromSource()) {
          usePool.add(Triple.of(n, EdgeType.IMPLEMENT_INTERFACE, interfaces[i].getQualifiedName()));
        }
      }
    }

    if (!td.bodyDeclarations().isEmpty()) {
      RelationNode body = new RelationNode(GraphUtil.popNodeID(graph), NodeType.BLOCK, "{}");
      graph.addVertex(body);
      defPool.put(qname + ".BLOCK", body);
      graph.addEdge(n, body, new Edge(GraphUtil.popEdgeID(graph), EdgeType.BODY));

      List<Initializer> initializers =
          ((List<?>) td.bodyDeclarations())
              .stream()
                  .filter(Initializer.class::isInstance)
                  .map(Initializer.class::cast)
                  .collect(Collectors.toList());
      if (!initializers.isEmpty()) {
        for (Initializer initializer : initializers) {
          if (!initializer.getBody().statements().isEmpty()) {
            ElementNode initNode =
                new ElementNode(
                    GraphUtil.popNodeID(graph),
                    NodeType.INIT_BLOCK_DECLARATION,
                    initializer.toString(),
                    td.getName().toString() + ".INIT",
                    qname + ".INIT");
            graph.addVertex(initNode);
            graph.addEdge(body, initNode, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CHILD));

            RelationNode initBlock = parseBodyBlock(initializer.getBody(), qname + ".BLOCK");
            graph.addEdge(
                initNode, initBlock, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CHILD));
          }
        }
      }

      IVariableBinding[] fdBindings = tdBinding.getDeclaredFields();
      IMethodBinding[] mdBindings = tdBinding.getDeclaredMethods();
      for (IVariableBinding b : fdBindings) {
        usePool.add(Triple.of(body, EdgeType.CHILD, qname + "." + b.getName()));
      }
      for (IMethodBinding b : mdBindings) {
        if (b.isDefaultConstructor()) {
          continue;
        }
        usePool.add(Triple.of(body, EdgeType.CHILD, getMethodQNameFromBinding(b)));
      }
    }

    return true;
  }

  public boolean visit(EnumDeclaration ed) {
    return true;
  }

  public boolean visit(FieldDeclaration fd) {
    List<VariableDeclarationFragment> fragments = fd.fragments();
    for (VariableDeclarationFragment fragment : fragments) {
      String name = fragment.getName().getFullyQualifiedName();
      String qname = name;
      IVariableBinding binding = fragment.resolveBinding();
      if (binding != null && binding.getDeclaringClass() != null) {
        qname = binding.getDeclaringClass().getQualifiedName() + "." + name;
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
    if (tpBinding != null && tpBinding.isFromSource()) {
      usePool.add(Triple.of(n, EdgeType.METHOD_RETURN, tpBinding.getQualifiedName()));
    }

    // para decl and type
    if (!md.parameters().isEmpty()) {
      // create element nodes
      List<SingleVariableDeclaration> paras = md.parameters();
      for (SingleVariableDeclaration p : paras) {
        String para_name = p.getName().getFullyQualifiedName();
        String para_qname = para_name;
        IVariableBinding b = p.resolveBinding();
        if (b != null && b.getDeclaringClass() != null) {
          para_qname = b.getDeclaringClass().getQualifiedName() + "." + para_name;
        }
        ElementNode pn =
            new ElementNode(
                GraphUtil.popNodeID(graph),
                NodeType.VAR_DECLARATION,
                p.toString(),
                para_name,
                para_qname);
        graph.addVertex(pn);
        graph.addEdge(n, pn, new Edge(GraphUtil.popEdgeID(graph), EdgeType.METHOD_PARAMETER));
        defPool.put(para_qname, pn);

        ITypeBinding paraBinding = p.getType().resolveBinding();
        if (paraBinding != null) {
          usePool.add(Triple.of(pn, EdgeType.DATA_TYPE, paraBinding.getQualifiedName()));
        }
      }
    }

    // TODO: add exception types
    if (!md.thrownExceptionTypes().isEmpty()) {}

    // TODO: process body here or else where?
    if (!md.getBody().statements().isEmpty()) {
      RelationNode blockNode = parseBodyBlock(md.getBody(), qname + ".BLOCK");
      graph.addEdge(n, blockNode, new Edge(GraphUtil.popEdgeID(graph), EdgeType.BODY));
    }
    return true;
  }

  /**
   * Parse the body for method/constructor, initializer block, and nested block
   *
   * @param body
   * @return
   */
  private RelationNode parseBodyBlock(Block body, String rootName) {
    RelationNode blockNode = new RelationNode(GraphUtil.popNodeID(graph), NodeType.BLOCK, "{}");
    graph.addVertex(blockNode);
    defPool.put(rootName, blockNode);

    List<Statement> statementList = body.statements();
    List<Statement> statements = new ArrayList<>();
    for (int i = 0; i < statementList.size(); i++) {
      statements.add(statementList.get(i));
    }

    for (int i = 0; i < statements.size(); i++) {
      Statement stmt = statements.get(i);

      // TODO: for nested block/statements, how to connect to parent?
      if (stmt.getNodeType() == ASTNode.BLOCK) {
        List<Statement> blockStatements = ((Block) stmt).statements();
        for (int j = 0; j < blockStatements.size(); j++) {
          statements.add(i + j + 1, blockStatements.get(j));
        }
        continue;
      }

      if (stmt.getNodeType() == ASTNode.RETURN_STATEMENT) {
        Expression expression = ((ReturnStatement) stmt).getExpression();
        if (expression != null) {
          parseExpression(expression);
        }
      }

      // control
      if (stmt.getNodeType() == ASTNode.IF_STATEMENT) {}
      if (stmt.getNodeType() == ASTNode.DO_STATEMENT) {}
      if (stmt.getNodeType() == ASTNode.FOR_STATEMENT) {}
      if (stmt.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT) {}
      if (stmt.getNodeType() == ASTNode.SWITCH_STATEMENT) {}
      if (stmt.getNodeType() == ASTNode.TRY_STATEMENT) {}

      if (stmt.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
        VariableDeclarationStatement vd = (VariableDeclarationStatement) stmt;
        for (Iterator iter = vd.fragments().iterator(); iter.hasNext(); ) {
          VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
          IVariableBinding binding = fragment.resolveBinding();
          String name = fragment.getName().getFullyQualifiedName();
          String qname = name;
          if (binding != null && binding.getType().isFromSource()) {
            String parentMethodName = getMethodQNameFromBinding(binding.getDeclaringMethod());
            qname = binding.getType().getQualifiedName() + "." + parentMethodName + "." + name;
            ElementNode n =
                new ElementNode(
                    GraphUtil.popNodeID(graph),
                    NodeType.VAR_DECLARATION,
                    fragment.toString(),
                    name,
                    qname);
            graph.addVertex(n);
            defPool.put(qname, n);

            usePool.add(Triple.of(n, EdgeType.PARENT, rootName));
            usePool.add(Triple.of(n, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
          }
        }
      }

      if (stmt.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {}
    }
    return blockNode;
  }

  private void parseExpression(Expression exp) {
    if (exp.getNodeType() == ASTNode.FIELD_ACCESS) {}
    if (exp.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {}
    if (exp.getNodeType() == ASTNode.METHOD_INVOCATION) {
      // get caller qname
      MethodInvocation mi = (MethodInvocation) exp;
      List<Expression> arguments = mi.arguments();

      // get callee qname
      IMethodBinding mdBinding = mi.resolveMethodBinding();
      // only internal invocation (or consider types, fields and local?)
      if (mdBinding != null) {
        RelationNode n =
            new RelationNode(GraphUtil.popNodeID(graph), NodeType.METHOD_INVOCATION, mi.toString());
        graph.addVertex(n);

        // called method
        mdBinding.getMethodDeclaration();
        //        usePool.add(Triple.of(n, EdgeType.METHOD_CALLER, findParentByType(mi,
        // ASTNode.METHOD_DECLARATION)));
        usePool.add(Triple.of(n, EdgeType.METHOD_CALLEE, getMethodQNameFromBinding(mdBinding)));
      }
    }
    if (exp.getNodeType() == ASTNode.ASSIGNMENT) {}
    if (exp.getNodeType() == ASTNode.CAST_EXPRESSION) {}
  }

  private String getMethodQNameFromBinding(IMethodBinding binding) {
    ITypeBinding[] paraBindings = binding.getParameterTypes();
    List<String> paraTypes = new ArrayList<>();
    for (ITypeBinding b : paraBindings) {
      paraTypes.add(b.getQualifiedName());
    }

    String qname = binding.getName() + "(" + String.join(",", paraTypes).trim() + ")";
    if (binding != null) {
      qname = binding.getDeclaringClass().getQualifiedName() + "." + qname;
    }
    return qname;
  }
}
