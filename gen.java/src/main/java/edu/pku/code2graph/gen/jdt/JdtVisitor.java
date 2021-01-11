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
    ElementNode node =
        new ElementNode(
            GraphUtil.popNodeID(graph), type, td.toString(), td.getName().toString(), qname);
    graph.addVertex(node);
    defPool.put(qname, node);

    parseExtendsAndImplements(tdBinding, node);
    parseMembers(tdBinding, node, td.bodyDeclarations());

    return true;
  }

  public boolean visit(EnumDeclaration ed) {
    ITypeBinding edBinding = ed.resolveBinding();
    assert edBinding != null;
    String qname = edBinding.getQualifiedName();
    ElementNode node =
        new ElementNode(
            GraphUtil.popNodeID(graph),
            NodeType.ENUM_DECLARATION,
            ed.toString(),
            ed.getName().toString(),
            qname);
    graph.addVertex(node);
    defPool.put(qname, node);

    for (Iterator<EnumConstantDeclaration> iter = ed.enumConstants().iterator(); iter.hasNext(); ) {
      EnumConstantDeclaration cst = iter.next();
      qname = qname + "." + cst.getName().toString();
      ElementNode cstNode =
          new ElementNode(
              GraphUtil.popNodeID(graph),
              NodeType.ENUM_CONSTANT_DECLARATION,
              cst.toString(),
              cst.getName().toString(),
              qname);
      graph.addVertex(cstNode);
      graph.addEdge(node, cstNode, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CHILD));
      defPool.put(qname, cstNode);
    }
    parseExtendsAndImplements(edBinding, node);
    parseMembers(edBinding, node, ed.bodyDeclarations());
    return true;
  }

  /**
   * Parse super class and interfaces for enum, type and interface declaration
   *
   * @param binding
   * @param node
   */
  private void parseExtendsAndImplements(ITypeBinding binding, ElementNode node) {
    ITypeBinding parentType = binding.getSuperclass();
    if (parentType != null && parentType.isFromSource()) {
      usePool.add(Triple.of(node, EdgeType.PARENT_CLASS, parentType.getQualifiedName()));
    }
    ITypeBinding[] interfaces = binding.getInterfaces();
    if (interfaces != null && interfaces.length > 0) {
      for (int i = 0; i < interfaces.length; ++i) {
        if (interfaces[i].isFromSource()) {
          usePool.add(Triple.of(node, EdgeType.INTERFACE, interfaces[i].getQualifiedName()));
        }
      }
    }
  }

  /**
   * Parse the members of body declarations, just the declaration signature, visit body block in
   * other visitors
   *
   * @param binding
   * @param node
   */
  private void parseMembers(
      ITypeBinding binding, ElementNode node, List<BodyDeclaration> bodyDeclarations) {
    if (bodyDeclarations.isEmpty()) {
      return;
    }
    String parentQName = node.getQualifiedName();
    RelationNode body = new RelationNode(GraphUtil.popNodeID(graph), NodeType.BLOCK, "{}");
    graph.addVertex(body);
    defPool.put(parentQName + ".BLOCK", body);
    graph.addEdge(node, body, new Edge(GraphUtil.popEdgeID(graph), EdgeType.BODY));

    List<Initializer> initializers =
        bodyDeclarations.stream()
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
                  node.getName() + ".INIT",
                  parentQName + ".INIT");
          graph.addVertex(initNode);
          graph.addEdge(body, initNode, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CHILD));

          parseBodyBlock(initializer.getBody(), parentQName + ".BLOCK")
              .ifPresent(
                  initBlock ->
                      graph.addEdge(
                          initNode,
                          initBlock,
                          new Edge(GraphUtil.popEdgeID(graph), EdgeType.CHILD)));
        }
      }
    }

    IVariableBinding[] fdBindings = binding.getDeclaredFields();
    for (IVariableBinding b : fdBindings) {
      usePool.add(Triple.of(body, EdgeType.CHILD, parentQName + "." + b.getName()));
    }

    IMethodBinding[] mdBindings = binding.getDeclaredMethods();
    for (IMethodBinding b : mdBindings) {
      if (b.isDefaultConstructor()) {
        continue;
      }
      usePool.add(Triple.of(body, EdgeType.CHILD, getMethodQNameFromBinding(b)));
    }
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
      ElementNode node =
          new ElementNode(
              GraphUtil.popNodeID(graph),
              NodeType.FIELD_DECLARATION,
              fragment.toString(),
              name,
              qname);
      graph.addVertex(node);
      defPool.put(qname, node);

      if (binding != null && binding.getType().isFromSource()) {
        usePool.add(Triple.of(node, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
      }

      if (fragment.getInitializer() != null) {
        graph.addEdge(
            node,
            parseExpression(fragment.getInitializer()),
            new Edge(GraphUtil.popEdgeID(graph), EdgeType.INITIALIZER));
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
    ElementNode node =
        new ElementNode(
            GraphUtil.popNodeID(graph), NodeType.METHOD_DECLARATION, md.toString(), name, qname);
    graph.addVertex(node);
    defPool.put(qname, node);

    // return type
    ITypeBinding tpBinding = mdBinding.getReturnType();
    if (tpBinding != null && tpBinding.isFromSource()) {
      usePool.add(Triple.of(node, EdgeType.METHOD_RETURN, tpBinding.getQualifiedName()));
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
        graph.addEdge(node, pn, new Edge(GraphUtil.popEdgeID(graph), EdgeType.METHOD_PARAMETER));
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
    if (md.getBody() != null) {
      if (!md.getBody().statements().isEmpty()) {
        parseBodyBlock(md.getBody(), qname + ".BLOCK")
            .ifPresent(
                blockNode ->
                    graph.addEdge(
                        node, blockNode, new Edge(GraphUtil.popEdgeID(graph), EdgeType.BODY)));
      }
    }
    return true;
  }

  /**
   * Parse the body for method/constructor, initializer block, and nested block
   *
   * @param body
   * @return
   */
  private Optional<RelationNode> parseBodyBlock(Block body, String rootName) {
    if (body == null || body.statements().isEmpty()) {
      return Optional.empty();
    }

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

    return Optional.of(root);
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
            if (binding != null) { // since it is declaration, binding should never be null
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

              if (fragment.getInitializer() != null) {
                graph.addEdge(
                    node,
                    parseExpression(fragment.getInitializer()),
                    new Edge(GraphUtil.popEdgeID(graph), EdgeType.INITIALIZER));
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

        // control
      case ASTNode.IF_STATEMENT:
        {
          IfStatement ifStatement = (IfStatement) stmt;
          //      ifStatement.getStartPosition();
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
                        graph.addEdge(
                            node, then, new Edge(GraphUtil.popEdgeID(graph), EdgeType.THEN)));
          }
          if (ifStatement.getElseStatement() != null) {
            parseStatement(ifStatement.getThenStatement())
                .ifPresent(
                    els ->
                        graph.addEdge(
                            node, els, new Edge(GraphUtil.popEdgeID(graph), EdgeType.ELSE)));
          }
          return Optional.of(node);
        }
      case ASTNode.FOR_STATEMENT:
        {
          ForStatement forStatement = (ForStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.popNodeID(graph), NodeType.FOR_STATEMENT, forStatement.toString());
          graph.addVertex(node);

          forStatement
              .initializers()
              .forEach(
                  init ->
                      graph.addEdge(
                          node,
                          parseExpression((Expression) init),
                          new Edge(GraphUtil.popEdgeID(graph), EdgeType.INITIALIZER)));
          forStatement
              .updaters()
              .forEach(
                  upd ->
                      graph.addEdge(
                          node,
                          parseExpression((Expression) upd),
                          new Edge(GraphUtil.popEdgeID(graph), EdgeType.UPDATER)));

          graph.addEdge(
              node,
              parseExpression(forStatement.getExpression()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.CONDITION));
          parseStatement(forStatement.getBody())
              .ifPresent(
                  body ->
                      graph.addEdge(
                          node, body, new Edge(GraphUtil.popEdgeID(graph), EdgeType.BODY)));

          return Optional.of(node);
        }

      case ASTNode.ENHANCED_FOR_STATEMENT:
        {
          EnhancedForStatement eForStatement = (EnhancedForStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.popNodeID(graph),
                  NodeType.ENHANCED_FOR_STATEMENT,
                  eForStatement.toString());
          graph.addVertex(node);

          SingleVariableDeclaration p = eForStatement.getParameter();
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
          graph.addEdge(node, pn, new Edge(GraphUtil.popEdgeID(graph), EdgeType.ELEMENT));
          defPool.put(para_qname, pn);

          ITypeBinding paraBinding = p.getType().resolveBinding();
          if (paraBinding != null) {
            usePool.add(Triple.of(pn, EdgeType.DATA_TYPE, paraBinding.getQualifiedName()));
          }

          graph.addEdge(
              node,
              parseExpression(eForStatement.getExpression()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.VALUES));
          parseStatement(eForStatement.getBody())
              .ifPresent(
                  body ->
                      graph.addEdge(
                          node, body, new Edge(GraphUtil.popEdgeID(graph), EdgeType.BODY)));

          return Optional.of(node);
        }
      case ASTNode.DO_STATEMENT:
        {
          DoStatement doStatement = ((DoStatement) stmt);
          RelationNode node =
              new RelationNode(
                  GraphUtil.popNodeID(graph), NodeType.DO_STATEMENT, doStatement.toString());
          graph.addVertex(node);

          Expression expression = doStatement.getExpression();
          if (expression != null) {
            RelationNode cond = parseExpression(expression);
            graph.addEdge(node, cond, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CONDITION));
          }

          Statement doBody = doStatement.getBody();
          if (doBody != null) {
            parseStatement(doBody)
                .ifPresent(
                    body ->
                        graph.addEdge(
                            node, body, new Edge(GraphUtil.popEdgeID(graph), EdgeType.BODY)));
          }
          return Optional.of(node);
        }
      case ASTNode.WHILE_STATEMENT:
        {
          WhileStatement whileStatement = (WhileStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.popNodeID(graph), NodeType.WHILE_STATEMENT, whileStatement.toString());
          graph.addVertex(node);

          Expression expression = whileStatement.getExpression();
          if (expression != null) {
            RelationNode cond = parseExpression(expression);
            graph.addEdge(node, cond, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CONDITION));
          }

          Statement whileBody = whileStatement.getBody();
          if (whileBody != null) {
            parseStatement(whileBody)
                .ifPresent(
                    body ->
                        graph.addEdge(
                            node, body, new Edge(GraphUtil.popEdgeID(graph), EdgeType.BODY)));
          }
          return Optional.of(node);
        }
      case ASTNode.SWITCH_STATEMENT:
        {
        }
      case ASTNode.TRY_STATEMENT:
        {
        }
    }

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
      case ASTNode.NUMBER_LITERAL:
      case ASTNode.STRING_LITERAL:
      case ASTNode.CHARACTER_LITERAL:
      case ASTNode.BOOLEAN_LITERAL:
      case ASTNode.NULL_LITERAL:
      case ASTNode.TYPE_LITERAL:
        {
          root.setSymbol(exp.toString());
          root.setType(NodeType.LITERAL);
          break;
        }
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
      case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
        {
          // TODO use another type for relation node
          root.setType(NodeType.VAR_DECLARATION);
          List<VariableDeclarationFragment> fragments =
              ((VariableDeclarationExpression) exp).fragments();
          for (VariableDeclarationFragment fragment : fragments) {
            String name = fragment.getName().getFullyQualifiedName();
            String qname = name;
            IVariableBinding binding = fragment.resolveBinding();
            if (binding != null && binding.getDeclaringMethod() != null) {
              qname = getMethodQNameFromBinding(binding.getDeclaringMethod()) + "." + name;
            }
            ElementNode n =
                new ElementNode(
                    GraphUtil.popNodeID(graph),
                    NodeType.VAR_DECLARATION,
                    fragment.toString(),
                    name,
                    qname);
            graph.addVertex(n);
            graph.addEdge(root, n, new Edge(GraphUtil.popEdgeID(graph), EdgeType.CHILD));
            defPool.put(qname, n);

            if (binding != null && binding.getType().isFromSource()) {
              usePool.add(Triple.of(n, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
            }
          }
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

          // accessor
          if (mi.getExpression() != null) {
            graph.addEdge(
                root,
                parseExpression(mi.getExpression()),
                new Edge(GraphUtil.popEdgeID(graph), EdgeType.ACCESSOR));
          }

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

          root.setType(NodeType.ASSIGNMENT);
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
          CastExpression castExpression = (CastExpression) exp;

          root.setType(NodeType.CAST_EXPRESSION);
          root.setSymbol("()");
          root.setArity(2);

          ITypeBinding typeBinding = castExpression.getType().resolveBinding();
          if (typeBinding != null && typeBinding.isFromSource()) {
            usePool.add(Triple.of(root, EdgeType.TARGET_TYPE, typeBinding.getQualifiedName()));
          }

          graph.addEdge(
              root,
              parseExpression(castExpression.getExpression()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.CASTED_OBJECT));

          break;
        }
      case ASTNode.INFIX_EXPRESSION:
        {
          InfixExpression iex = (InfixExpression) exp;
          root.setType(NodeType.INFIX);
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
          PrefixExpression pex = (PrefixExpression) exp;
          root.setType(NodeType.PREFIX);
          root.setSymbol(pex.getOperator().toString());
          root.setArity(1);

          graph.addEdge(
              root,
              parseExpression(pex.getOperand()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.LEFT));
          break;
        }
      case ASTNode.POSTFIX_EXPRESSION:
        {
          PostfixExpression pex = (PostfixExpression) exp;
          root.setType(NodeType.POSTFIX);
          root.setSymbol(pex.getOperator().toString());
          root.setArity(1);

          graph.addEdge(
              root,
              parseExpression(pex.getOperand()),
              new Edge(GraphUtil.popEdgeID(graph), EdgeType.RIGHT));
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
