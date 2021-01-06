package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.eclipse.jdt.core.dom.*;
import org.jgrapht.alg.util.Triple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
          usePool.add(Triple.of(n, EdgeType.IMPLEMENTATION, interfaces[i].getQualifiedName()));
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
        if (b != null && b.getVariableDeclaration() != null) {
          para_qname = getMethodQNameFromBinding(b.getDeclaringMethod()) + "." + para_name;
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
    //    if(body.statements().isEmpty()){
    //      return null;
    //    }
    // the node of the current block node
    RelationNode root = new RelationNode(GraphUtil.popNodeID(graph), NodeType.BLOCK, "{}");
    graph.addVertex(root);
    defPool.put(rootName, root);

    List<Statement> statementList = body.statements();
    List<Statement> statements = new ArrayList<>();
    for (int i = 0; i < statementList.size(); i++) {
      statements.add(statementList.get(i));
    }

    for (int i = 0; i < statements.size(); i++) {
      Statement stmt = statements.get(i);

      // TODO: for nested block/statements, how to connect to parent?
      // iteration, use line number to uniquely identify
      if (stmt.getNodeType() == ASTNode.BLOCK) {
        //        parseBodyBlock((Block)stmt, )
        continue;
      }

      parseStatement(stmt)
          .ifPresent(
              node ->
                  graph.addEdge(root, node, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CHILD)));
    }

    return root;
  }

  /**
   * Parse statement and return the created node
   *
   * @param stmt
   * @return
   */
  private Optional<Node> parseStatement(Statement stmt) {
    switch (stmt.getNodeType()) {
      case ASTNode.BLOCK:
        {
          Block block = (Block) stmt;
          if (block.statements().isEmpty()) {
            return Optional.empty();
          } else {
            RelationNode node = new RelationNode(GraphUtil.popNodeID(graph), NodeType.BLOCK, "{}");
            graph.addVertex(node);
            for (Object st : block.statements()) {
              parseStatement((Statement) st)
                  .ifPresent(
                      snode ->
                          graph.addEdge(
                              node, snode, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CHILD)));
            }
            return Optional.of(node);
          }
        }
      case ASTNode.RETURN_STATEMENT:
        {
          Expression expression = ((ReturnStatement) stmt).getExpression();
          if (expression != null) {
            RelationNode node = parseExpression(expression);
            graph.addVertex(node);
            return Optional.of(node);
          }
          break;
        }
      case ASTNode.VARIABLE_DECLARATION_STATEMENT:
        {
          // local var declaration
          VariableDeclarationStatement vd = (VariableDeclarationStatement) stmt;
          for (Iterator iter = vd.fragments().iterator(); iter.hasNext(); ) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
            IVariableBinding binding = fragment.resolveBinding();
            String name = fragment.getName().getFullyQualifiedName();
            String qname = name;
            if (binding != null) {
              String parentMethodName = getMethodQNameFromBinding(binding.getDeclaringMethod());
              qname = parentMethodName + "." + name;
              ElementNode node =
                  new ElementNode(
                      GraphUtil.popNodeID(graph),
                      NodeType.VAR_DECLARATION,
                      fragment.toString(),
                      name,
                      qname);
              graph.addVertex(node);
              defPool.put(qname, node);

              if (binding.getType().isFromSource()) {
                usePool.add(
                    Triple.of(node, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
              }
              return Optional.of(node);
            }
          }
          break;
        }
      case ASTNode.EXPRESSION_STATEMENT:
        {
          Expression exp = ((ExpressionStatement) stmt).getExpression();
          if (exp != null) {
            RelationNode node = parseExpression(exp);
            return Optional.of(node);
          }
          break;
        }
    }

    // control
    if (stmt.getNodeType() == ASTNode.IF_STATEMENT) {
      IfStatement ifStatement = (IfStatement) stmt;
      ifStatement.getStartPosition();
      RelationNode node =
          new RelationNode(
              GraphUtil.popNodeID(graph), NodeType.IF_STATEMENT, ifStatement.toString());
      graph.addVertex(node);

      if (ifStatement.getExpression() != null) {
        RelationNode cond = parseExpression(ifStatement.getExpression());
        graph.addEdge(node, cond, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CONDITION));
      }
      if (ifStatement.getThenStatement() != null) {
        parseStatement(ifStatement.getThenStatement())
            .ifPresent(
                then ->
                    graph.addEdge(node, then, new Edge(GraphUtil.popEdgeID(graph), EdgeType.THEN)));
      }
      if (ifStatement.getElseStatement() != null) {
        parseStatement(ifStatement.getThenStatement())
            .ifPresent(
                els ->
                    graph.addEdge(node, els, new Edge(GraphUtil.popEdgeID(graph), EdgeType.ELSE)));
      }
      return Optional.of(node);
    }

    if (stmt.getNodeType() == ASTNode.DO_STATEMENT) {}
    if (stmt.getNodeType() == ASTNode.FOR_STATEMENT) {}
    if (stmt.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT) {}
    if (stmt.getNodeType() == ASTNode.SWITCH_STATEMENT) {}
    if (stmt.getNodeType() == ASTNode.TRY_STATEMENT) {}

    return Optional.empty();
  }

  /**
   * Expression at the leaf level, modeled as relation Link used vars and methods into its def, if
   * exists
   *
   * @param exp
   * @return
   */
  private RelationNode parseExpression(Expression exp) {
    RelationNode root = new RelationNode(GraphUtil.popNodeID(graph));
    root.setSnippet(exp.toString());
    graph.addVertex(root);

    // simple name may be self-field access
    switch (exp.getNodeType()) {
      case ASTNode.SIMPLE_NAME:
        {
          IBinding binding = ((SimpleName) exp).resolveBinding();
          if (binding != null && binding instanceof IVariableBinding) {
            IVariableBinding varBinding = (IVariableBinding) binding;
            if (varBinding.isField()) {
              root.setType(NodeType.FIELD_ACCESS);
              usePool.add(
                  Triple.of(
                      root,
                      EdgeType.REFERENCE,
                      varBinding.getDeclaringClass().getQualifiedName()
                          + "."
                          + varBinding.getName()));
            } else if (varBinding.isParameter()) {
              root.setType(NodeType.PARAMETER_ACCESS);
              usePool.add(
                  Triple.of(
                      root,
                      EdgeType.REFERENCE,
                      getMethodQNameFromBinding(varBinding.getDeclaringMethod())
                          + "."
                          + varBinding.getName()));
            } else if (varBinding.isEnumConstant()) {
              root.setType(NodeType.CONSTANT_ACCESS);
              usePool.add(
                  Triple.of(
                      root,
                      EdgeType.REFERENCE,
                      varBinding.getDeclaringClass().getQualifiedName()
                          + "."
                          + varBinding.getName()));
            } else {
              // if not the above 3, then must be a local variable
              root.setType(NodeType.LOCAL_VAR_ACCESS);
              usePool.add(
                  Triple.of(
                      root,
                      EdgeType.REFERENCE,
                      getMethodQNameFromBinding(varBinding.getDeclaringMethod())
                          + "."
                          + varBinding.getName()));
            }
          }
          break;
        }
      case ASTNode.THIS_EXPRESSION:
        {
          //          root.setType(NodeType.);
          ThisExpression te = (ThisExpression) exp;
          te.getQualifier();
          break;
        }
      case ASTNode.FIELD_ACCESS:
        {
          root.setType(NodeType.FIELD_ACCESS);
          FieldAccess fa = (FieldAccess) exp;

          IVariableBinding faBinding = fa.resolveFieldBinding();
          if (faBinding != null && faBinding.isField()) {
            usePool.add(
                Triple.of(
                    root,
                    EdgeType.REFERENCE,
                    faBinding.getDeclaringClass().getQualifiedName() + "." + faBinding.getName()));
          }
          break;
        }
      case ASTNode.CLASS_INSTANCE_CREATION:
        {
          root.setType(NodeType.TYPE_INSTANTIATION);
          IMethodBinding constructorBinding =
              ((ClassInstanceCreation) exp).resolveConstructorBinding();
          if (constructorBinding != null) {
            usePool.add(
                Triple.of(
                    root,
                    EdgeType.DATA_TYPE,
                    constructorBinding.getDeclaringClass().getQualifiedName()));
            // TODO: parse argument expressions
          }
          break;
        }
      case ASTNode.METHOD_INVOCATION:
        {
          MethodInvocation mi = (MethodInvocation) exp;
          root.setType(NodeType.METHOD_INVOCATION);

          // TODO: link arg use to its declaration (local, field, or external)
          List<Expression> arguments = mi.arguments();
          for (Expression arg : arguments) {}

          IMethodBinding mdBinding = mi.resolveMethodBinding();
          // only internal invocation (or consider types, fields and local?)
          if (mdBinding != null) {
            // get caller qname
            findWrappedMethod(mi)
                .ifPresent(
                    name -> {
                      usePool.add(Triple.of(root, EdgeType.METHOD_CALLER, name));
                    });
            // get callee qname
            usePool.add(
                Triple.of(root, EdgeType.METHOD_CALLEE, getMethodQNameFromBinding(mdBinding)));
          }
          break;
        }
      case ASTNode.ASSIGNMENT:
        {
          Assignment asg = (Assignment) exp;

          root.setType(NodeType.ASSIGNMENT_OPERATOR);
          root.setSymbol(asg.getOperator().toString());
          root.setArity(2);

          graph.addEdge(
              root,
              parseExpression(asg.getLeftHandSide()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.LEFT));
          graph.addEdge(
              root,
              parseExpression(asg.getRightHandSide()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.RIGHT));
          break;
        }
      case ASTNode.CAST_EXPRESSION:
        {
          break;
        }
      case ASTNode.INFIX_EXPRESSION:
        {
          InfixExpression iex = (InfixExpression) exp;
          root.setType(NodeType.INFIX_OPERATOR);
          root.setSymbol(iex.getOperator().toString());
          root.setArity(2);

          graph.addEdge(
              root,
              parseExpression(iex.getLeftOperand()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.LEFT));
          graph.addEdge(
              root,
              parseExpression(iex.getRightOperand()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.RIGHT));
          break;
        }
      case ASTNode.PREFIX_EXPRESSION:
        {
          break;
        }
      case ASTNode.POSTFIX_EXPRESSION:
        {
          break;
        }
    }

    return root;
  }

  /**
   * Find the nearest ancestor of a specific type, return its qname
   *
   * @return
   */
  private Optional<String> findWrappedMethod(ASTNode node) {
    ASTNode parent = node.getParent();
    while (parent != null) {
      if (parent.getNodeType() == ASTNode.METHOD_DECLARATION) {
        return Optional.of(
            getMethodQNameFromBinding(((MethodDeclaration) parent).resolveBinding()));
      }
      parent = parent.getParent();
    }
    return Optional.empty();
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
