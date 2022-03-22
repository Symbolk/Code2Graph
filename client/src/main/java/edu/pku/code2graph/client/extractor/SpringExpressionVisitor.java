package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.gen.jdt.AbstractJdtVisitor;
import edu.pku.code2graph.gen.jdt.ExpressionVisitor;
import edu.pku.code2graph.gen.jdt.JdtService;
import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.eclipse.jdt.core.dom.*;
import org.jgrapht.alg.util.Triple;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Visitor focusing on the entity granularity, at or above expression level
 * (cu/type/member/statement/expression)
 *
 * <p>1. visit, store and index all concerned nodes
 *
 * <p>2. create nodes with qualified name, and cache a mapping between qname and node
 *
 * <p>3. cache role and target node with triple of <node, role, qname>
 *
 * <p>5. create edges
 */
public class SpringExpressionVisitor extends ExpressionVisitor {
  private ElementNode cuNode;
  private Map<String, List<URI>> javaURIS;
  private String currentTemplate;
  private boolean currentReturnLiteral = false;
  private boolean toParseReturn = false;
  private List<URI> globalAttr = new ArrayList<>();
  private Map<String, List<URI>> viewPathReturns;

  public SpringExpressionVisitor(
      Map<String, List<URI>> uris, Map<String, List<URI>> viewPathReturns) {
    super();
    javaURIS = uris;
    this.viewPathReturns = viewPathReturns;
  }

  private void addViewPathReturn(URI uri) {
    if (!viewPathReturns.containsKey(currentTemplate))
      viewPathReturns.put(currentTemplate, new ArrayList<>());
    viewPathReturns.get(currentTemplate).add(uri);
  }

  @Override
  public boolean visit(MethodDeclaration md) {
    currentTemplate = "";
    currentReturnLiteral = false;
    // A binding represents a named entity in the Java language
    // for internal, should never be null
    IMethodBinding mdBinding = md.resolveBinding();
    // can be null for method in local anonymous class
    String name = md.getName().getFullyQualifiedName();
    String qname = name;
    if (mdBinding != null) {
      qname = JdtService.getMethodQNameFromBinding(mdBinding);
    }

    ElementNode node =
        createElementNode(
            NodeType.METHOD_DECLARATION, md.toString(), name, qname, JdtService.getIdentifier(md));

    node.setRange(computeRange(md));

    if (md.getBody() != null) {
      if (!md.getBody().statements().isEmpty()) {
        findTemplateOfMethod(md.getBody(), qname + ".BLOCK")
            .ifPresent(template -> logger.debug(template));
      }
    }

    if (!globalAttr.isEmpty() && globalAttr.get(0).getFile().equals(uriFilePath)) {
      for (URI uri : globalAttr) {
        if (currentTemplate != "" && !javaURIS.containsKey(currentTemplate)) {
          javaURIS.put(currentTemplate, new ArrayList<>());
          javaURIS.get(currentTemplate).add(uri);
        } else if (currentTemplate != "") {
          if (!javaURIS.get(currentTemplate).contains(uri)) javaURIS.get(currentTemplate).add(uri);
        }
      }
    } else {
      globalAttr.clear();
    }

    // annotations
    parseAnnotations(md.modifiers(), node);

    // para decl and type
    if (!md.parameters().isEmpty()) {
      // create element nodes
      List<SingleVariableDeclaration> paras = md.parameters();
      for (SingleVariableDeclaration p : paras) {
        if (p.toString().trim().startsWith("@ModelAttribute")) {
          String identifier = JdtService.getIdentifier(p);
          String[] split = p.toString().trim().split(" ");
          identifier = identifier.replace(".", "/").replaceAll("\\(.+?\\)", "");
          //          String[] idtfSplit = identifier.split("/");
          //          idtfSplit[idtfSplit.length - 1] = URI.checkInvalidCh(split[0]);
          //          identifier = String.join("/", idtfSplit);
          identifier = identifier + '/' + URI.checkInvalidCh(split[0]);

          String para_qname = split[0];
          String para_name =
              para_qname.substring(para_qname.indexOf('"') + 1, para_qname.length() - 2);

          URI uri = new URI(true, uriFilePath);
          uri.addLayer(identifier, Language.JAVA);

          ElementNode pn =
              new ElementNode(
                  GraphUtil.nid(),
                  Language.JAVA,
                  NodeType.VAR_DECLARATION,
                  p.toString(),
                  para_name,
                  para_qname,
                  uri);

          graph.addVertex(pn);
          GraphUtil.addNode(pn);
          graph.addEdge(node, pn, new Edge(GraphUtil.eid(), EdgeType.PARAMETER));

          if (currentTemplate != "" && !javaURIS.containsKey(currentTemplate)) {
            javaURIS.put(currentTemplate, new ArrayList<>());
            javaURIS.get(currentTemplate).add(uri);
          } else if (currentTemplate != "") {
            if (!javaURIS.get(currentTemplate).contains(uri))
              javaURIS.get(currentTemplate).add(uri);
          }
        }

        String para_name = p.getName().getFullyQualifiedName();
        String para_qname = para_name;
        IVariableBinding b = p.resolveBinding();
        if (b != null && b.getVariableDeclaration() != null) {
          para_qname =
              JdtService.getMethodQNameFromBinding(b.getDeclaringMethod()) + "." + para_name;
        }

        ElementNode pn =
            createElementNode(
                NodeType.VAR_DECLARATION,
                p.toString(),
                para_name,
                para_qname,
                JdtService.getIdentifier(p));

        node.setRange(computeRange(p));
        graph.addEdge(node, pn, new Edge(GraphUtil.eid(), EdgeType.PARAMETER));
      }
    }

    // TODO: add exception types
    if (!md.thrownExceptionTypes().isEmpty()) {}

    // TODO: process body here or else where?
    if (md.getBody() != null) {
      if (!md.getBody().statements().isEmpty()) {
        parseBodyBlock(md.getBody(), md.getName().toString(), qname)
            .ifPresent(
                blockNode ->
                    graph.addEdge(node, blockNode, new Edge(GraphUtil.eid(), EdgeType.BODY)));
      }
    }

    currentTemplate = "";
    currentReturnLiteral = false;
    return true;
  }

  private Optional<String> findTemplateOfMethod(Block body, String rootName) {
    if (body == null || body.statements().isEmpty()) {
      return Optional.empty();
    }

    // the node of the current block node
    Optional<String> rootOpt = findTemplateOfMethod(body);
    if (rootOpt.isPresent()) {
      return rootOpt;
    } else {
      return Optional.empty();
    }
  }

  private Optional<String> findTemplateOfMethod(Block body) {
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

      if (stmt.getNodeType() == ASTNode.RETURN_STATEMENT) {
        Expression expression = ((ReturnStatement) stmt).getExpression();
        if (expression != null && expression.getNodeType() == ASTNode.STRING_LITERAL) {
          Pattern pathRegex = Pattern.compile("[\\w-/]+");
          String returnExpr = expression.toString();
          if (returnExpr != null
              && !returnExpr.trim().isEmpty()
              && pathRegex.matcher(returnExpr).find()) {
            currentTemplate = returnExpr.substring(1, returnExpr.length() - 1);
            currentReturnLiteral = true;
            return Optional.of(returnExpr);
          }
        } else if (expression != null
            && expression.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
          // For specific repos
          if (!((ClassInstanceCreation) expression).arguments().isEmpty()
              && ((Expression) ((ClassInstanceCreation) expression).arguments().get(0))
                      .getNodeType()
                  == ASTNode.STRING_LITERAL) {
            Pattern pathRegex = Pattern.compile("[\\w-/]+");
            String returnExpr =
                ((Expression) ((ClassInstanceCreation) expression).arguments().get(0)).toString();
            if (returnExpr != null
                && !returnExpr.trim().isEmpty()
                && pathRegex.matcher(returnExpr).find()) {
              currentTemplate = returnExpr.substring(1, returnExpr.length() - 1);
              currentReturnLiteral = true;
              return Optional.of(returnExpr);
            }
          }
        } else if (expression != null && expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
          // For specific repos
          if (!((MethodInvocation) expression).arguments().isEmpty()
              && ((Expression) ((MethodInvocation) expression).arguments().get(0)).getNodeType()
                  == ASTNode.STRING_LITERAL) {
            Pattern pathRegex = Pattern.compile("[\\w-/]+");
            String returnExpr =
                ((Expression) ((MethodInvocation) expression).arguments().get(0)).toString();
            if (returnExpr != null
                && !returnExpr.trim().isEmpty()
                && pathRegex.matcher(returnExpr).find()
                && !returnExpr.equals("\".html\"")
                && !returnExpr.equals("\".jsp\"")) {
              currentTemplate = returnExpr.substring(1, returnExpr.length() - 1);
              currentReturnLiteral = true;
              return Optional.of(returnExpr);
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  @Override
  protected void parseAnnotations(List modifiers, ElementNode annotatedNode) {
    for (Object modifier : modifiers) {
      if (modifier instanceof Annotation) {
        Annotation annotation = (Annotation) modifier;
        // create node as a child of annotatedNode
        RelationNode node =
            new RelationNode(
                GraphUtil.nid(),
                Language.JAVA,
                NodeType.ANNOTATION,
                annotation.toString(),
                annotation.getTypeName().getFullyQualifiedName());
        node.setRange(computeRange(annotation));
        node.setUri(createIdentifier(identifier));

        graph.addVertex(node);
        graph.addEdge(annotatedNode, node, new Edge(GraphUtil.eid(), EdgeType.ANNOTATION));
      }
    }
  }

  /**
   * Expression at the leaf level, modeled as relation Link used vars and methods into its def, if
   * exists
   *
   * @param exp
   * @return
   */
  @Override
  protected RelationNode parseExpression(Expression exp) {
    RelationNode root = new RelationNode(GraphUtil.nid(), Language.JAVA);
    root.setRange(computeRange(exp));
    root.setSnippet(exp.toString());
    graph.addVertex(root);

    // simple name may be self-field access
    switch (exp.getNodeType()) {
      case ASTNode.NUMBER_LITERAL:
      case ASTNode.CHARACTER_LITERAL:
      case ASTNode.BOOLEAN_LITERAL:
      case ASTNode.NULL_LITERAL:
      case ASTNode.TYPE_LITERAL:
        {
          root.setSymbol(exp.toString());
          root.setType(NodeType.LITERAL);
          break;
        }
      case ASTNode.STRING_LITERAL:
        {
          root.setSymbol(exp.toString());
          root.setType(NodeType.LITERAL);
          String content = URI.checkInvalidCh(((StringLiteral) exp).getLiteralValue());
          URI uri = createIdentifier(null, false);
          uri.addLayer(content);
          root.setUri(uri);
          if (currentReturnLiteral && toParseReturn) {
            if (currentTemplate.equals(exp.toString().substring(1, exp.toString().length() - 1))) {
              addViewPathReturn(uri);
            }
          }
          break;
        }
      case ASTNode.QUALIFIED_NAME:
        {
          QualifiedName name = (QualifiedName) exp;
          root.setType(NodeType.QUALIFIED_NAME);
          root.setUri(createIdentifier(name.getFullyQualifiedName()));
          GraphUtil.addNode(root);
          break;
        }
      case ASTNode.SIMPLE_NAME:
        {
          SimpleName name = (SimpleName) exp;
          String identifier = name.getFullyQualifiedName();
          IBinding binding = name.resolveBinding();
          root.setUri(createIdentifier(identifier));
          GraphUtil.addNode(root);
          if (binding == null) {
            // an unresolved identifier
            root.setType(NodeType.SIMPLE_NAME);
          } else if (binding instanceof IVariableBinding) {
            IVariableBinding varBinding = (IVariableBinding) binding;
            if (varBinding.isField()) {
              root.setType(NodeType.FIELD_ACCESS);
            } else if (varBinding.isParameter()) {
              root.setType(NodeType.PARAMETER_ACCESS);
            } else if (varBinding.isEnumConstant()) {
              root.setType(NodeType.CONSTANT_ACCESS);
            } else {
              // if not the above 3, then must be a local variable
              root.setType(NodeType.LOCAL_VAR_ACCESS);
            }
          }
          break;
        }
      case ASTNode.THIS_EXPRESSION:
        {
          root.setType(NodeType.THIS);
          //          ThisExpression te = (ThisExpression) exp;
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
            if (binding != null) {
              qname = JdtService.getVariableQNameFromBinding(binding, exp);
            }

            ElementNode n =
                createElementNode(
                    NodeType.VAR_DECLARATION,
                    fragment.toString(),
                    name,
                    qname,
                    JdtService.getIdentifier(fragment));

            n.setRange(computeRange(fragment));
            graph.addEdge(root, n, new Edge(GraphUtil.eid(), EdgeType.CHILD));
          }
          break;
        }
      case ASTNode.SUPER_FIELD_ACCESS:
        {
          SuperFieldAccess fa = (SuperFieldAccess) exp;
          root.setType(NodeType.SUPER_FIELD_ACCESS);
          root.setUri(createIdentifier(fa.toString()));
          break;
        }
      case ASTNode.FIELD_ACCESS:
        {
          root.setType(NodeType.FIELD_ACCESS);
          break;
        }
      case ASTNode.CLASS_INSTANCE_CREATION:
        {
          root.setType(NodeType.TYPE_INSTANTIATION);

          ClassInstanceCreation cic = (ClassInstanceCreation) exp;
          //          parseArguments(root, cic.arguments(), false);
          String identifier = cic.getType().toString();
          pushScope(identifier);
          parseArguments(root, cic.arguments());
          popScope();
          break;
        }
      case ASTNode.SUPER_METHOD_INVOCATION:
        {
          SuperMethodInvocation mi = (SuperMethodInvocation) exp;
          String identifier = "super." + mi.getName().toString();
          root.setType(NodeType.SUPER_METHOD_INVOCATION);
          root.setUri(createIdentifier(identifier));

          //          parseArguments(root, mi.arguments(), false);
          pushScope(identifier);
          parseArguments(root, mi.arguments());
          popScope();
          break;
        }
      case ASTNode.METHOD_INVOCATION:
        {
          MethodInvocation mi = (MethodInvocation) exp;

          // accessor
          Expression expr = mi.getExpression();
          if (expr != null) {
            Edge edge = new Edge(GraphUtil.eid(), EdgeType.ACCESSOR);
            graph.addEdge(root, parseExpression(expr), edge);
            String source = expr.toString();
            if (source.matches("^\\w+(\\.\\w+)*$")) {
              identifier = expr.toString() + "." + mi.getName().getIdentifier();
            } else {
              identifier = "." + mi.getName().getIdentifier();
            }
          } else {
            identifier = mi.getName().getIdentifier();
          }

          root.setType(NodeType.METHOD_INVOCATION);
          root.setUri(createIdentifier(identifier));
          GraphUtil.addNode(root);

          pushScope(identifier);
          parseArguments(root, mi.arguments());
          popScope();

          if (exp.toString().contains("addAttribute") || exp.toString().contains("setAttribute")) {
            //          parseArguments(root, mi.arguments(), addedToMap);
            String identifier = root.getUri().getIdentifier();
            String arg = mi.arguments().get(0).toString();
            URI uri = new URI(false, uriFilePath);
            uri.addLayer(identifier, Language.JAVA);
            uri.addLayer(arg.substring(1, arg.length() - 1), Language.ANY);

            if (currentTemplate != "" && !javaURIS.containsKey(currentTemplate)) {
              javaURIS.put(currentTemplate, new ArrayList<>());
              javaURIS.get(currentTemplate).add(uri);
            } else if (currentTemplate != "") {
              if (!javaURIS.get(currentTemplate).contains(uri))
                javaURIS.get(currentTemplate).add(uri);
            }
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
              new Edge(GraphUtil.eid(), EdgeType.LEFT));
          graph.addEdge(
              root,
              parseExpression(asg.getRightHandSide()),
              new Edge(GraphUtil.eid(), EdgeType.RIGHT));
          break;
        }
      case ASTNode.CAST_EXPRESSION:
        {
          CastExpression castExpression = (CastExpression) exp;

          root.setType(NodeType.CAST_EXPRESSION);
          root.setSymbol("()");
          root.setArity(2);

          graph.addEdge(
              root,
              parseExpression(castExpression.getExpression()),
              new Edge(GraphUtil.eid(), EdgeType.CASTED_OBJECT));

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
              new Edge(GraphUtil.eid(), EdgeType.LEFT));
          graph.addEdge(
              root,
              parseExpression(iex.getRightOperand()),
              new Edge(GraphUtil.eid(), EdgeType.RIGHT));
          break;
        }
      case ASTNode.PREFIX_EXPRESSION:
        {
          PrefixExpression pex = (PrefixExpression) exp;
          root.setType(NodeType.PREFIX);
          root.setSymbol(pex.getOperator().toString());
          root.setArity(1);

          graph.addEdge(
              root, parseExpression(pex.getOperand()), new Edge(GraphUtil.eid(), EdgeType.LEFT));
          break;
        }
      case ASTNode.POSTFIX_EXPRESSION:
        {
          PostfixExpression pex = (PostfixExpression) exp;
          root.setType(NodeType.POSTFIX);
          root.setSymbol(pex.getOperator().toString());
          root.setArity(1);

          graph.addEdge(
              root, parseExpression(pex.getOperand()), new Edge(GraphUtil.eid(), EdgeType.RIGHT));
        }
    }

    return root;
  }

  @Override
  protected Optional<Node> parseStatement(Statement stmt) {
    switch (stmt.getNodeType()) {
      case ASTNode.BLOCK:
        {
          Block block = (Block) stmt;
          if (block.statements().isEmpty()) {
            return Optional.empty();
          } else {
            RelationNode node =
                new RelationNode(GraphUtil.nid(), Language.JAVA, NodeType.BLOCK, "{}");
            node.setRange(computeRange(stmt));
            graph.addVertex(node);

            for (Object st : block.statements()) {
              parseStatement((Statement) st)
                  .ifPresent(
                      child ->
                          graph.addEdge(node, child, new Edge(GraphUtil.eid(), EdgeType.CHILD)));
            }
            return Optional.of(node);
          }
        }
        // it is strange that constructor invocations is statement instead of expression
      case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
        {
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(),
                  Language.JAVA,
                  NodeType.SUPER_CONSTRUCTOR_INVOCATION,
                  stmt.toString());
          node.setRange(computeRange(stmt));
          graph.addVertex(node);

          SuperConstructorInvocation ci = (SuperConstructorInvocation) stmt;
          pushScope("super");
          parseArguments(node, ci.arguments());
          popScope();
          return Optional.of(node);
        }
      case ASTNode.CONSTRUCTOR_INVOCATION:
        {
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(), Language.JAVA, NodeType.CONSTRUCTOR_INVOCATION, stmt.toString());
          node.setRange(computeRange(stmt));
          graph.addVertex(node);

          ConstructorInvocation ci = (ConstructorInvocation) stmt;
          pushScope("this");
          parseArguments(node, ci.arguments());
          popScope();
          return Optional.of(node);
        }
      case ASTNode.RETURN_STATEMENT:
        {
          Expression expression = ((ReturnStatement) stmt).getExpression();
          if (expression != null) {
            toParseReturn = true;
            pushScope("return");
            RelationNode node = parseExpression(expression);
            popScope();
            graph.addVertex(node);
            toParseReturn = false;
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
              qname = JdtService.getVariableQNameFromBinding(binding, stmt);

              ElementNode node =
                  createElementNode(
                      NodeType.VAR_DECLARATION,
                      fragment.toString(),
                      name,
                      qname,
                      JdtService.getIdentifier(fragment));

              node.setRange(computeRange(fragment));

              if (fragment.getInitializer() != null) {
                graph.addEdge(
                    node,
                    parseExpression(fragment.getInitializer()),
                    new Edge(GraphUtil.eid(), EdgeType.INITIALIZER));
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
                  GraphUtil.nid(), Language.JAVA, NodeType.IF_STATEMENT, ifStatement.toString());
          node.setRange(computeRange(stmt));

          graph.addVertex(node);

          if (ifStatement.getExpression() != null) {
            RelationNode cond = parseExpression(ifStatement.getExpression());
            graph.addEdge(node, cond, new Edge(GraphUtil.eid(), EdgeType.CONDITION));
          }
          if (ifStatement.getThenStatement() != null) {
            parseStatement(ifStatement.getThenStatement())
                .ifPresent(
                    then -> graph.addEdge(node, then, new Edge(GraphUtil.eid(), EdgeType.THEN)));
          }
          if (ifStatement.getElseStatement() != null) {
            parseStatement(ifStatement.getElseStatement())
                .ifPresent(
                    els -> graph.addEdge(node, els, new Edge(GraphUtil.eid(), EdgeType.ELSE)));
          }
          return Optional.of(node);
        }
      case ASTNode.FOR_STATEMENT:
        {
          ForStatement forStatement = (ForStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(), Language.JAVA, NodeType.FOR_STATEMENT, forStatement.toString());
          node.setRange(computeRange(stmt));

          graph.addVertex(node);

          forStatement
              .initializers()
              .forEach(
                  init ->
                      graph.addEdge(
                          node,
                          parseExpression((Expression) init),
                          new Edge(GraphUtil.eid(), EdgeType.INITIALIZER)));
          forStatement
              .updaters()
              .forEach(
                  upd ->
                      graph.addEdge(
                          node,
                          parseExpression((Expression) upd),
                          new Edge(GraphUtil.eid(), EdgeType.UPDATER)));

          if (forStatement.getExpression() != null) {
            graph.addEdge(
                node,
                parseExpression(forStatement.getExpression()),
                new Edge(GraphUtil.eid(), EdgeType.CONDITION));
          }
          parseStatement(forStatement.getBody())
              .ifPresent(
                  body -> graph.addEdge(node, body, new Edge(GraphUtil.eid(), EdgeType.BODY)));

          return Optional.of(node);
        }

      case ASTNode.ENHANCED_FOR_STATEMENT:
        {
          EnhancedForStatement eForStatement = (EnhancedForStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(),
                  Language.JAVA,
                  NodeType.ENHANCED_FOR_STATEMENT,
                  eForStatement.toString());
          node.setRange(computeRange(stmt));

          graph.addVertex(node);

          SingleVariableDeclaration p = eForStatement.getParameter();
          String para_name = p.getName().getFullyQualifiedName();
          String para_qname = para_name;
          IVariableBinding b = p.resolveBinding();
          if (b != null && b.getVariableDeclaration() != null) {
            para_qname =
                JdtService.getMethodQNameFromBinding(b.getDeclaringMethod()) + "." + para_name;
          }

          ElementNode pn =
              createElementNode(
                  NodeType.VAR_DECLARATION,
                  p.toString(),
                  para_name,
                  para_qname,
                  JdtService.getIdentifier(p));

          pn.setRange(computeRange(p));
          graph.addEdge(node, pn, new Edge(GraphUtil.eid(), EdgeType.ELEMENT));

          graph.addEdge(
              node,
              parseExpression(eForStatement.getExpression()),
              new Edge(GraphUtil.eid(), EdgeType.VALUES));
          parseStatement(eForStatement.getBody())
              .ifPresent(
                  body -> graph.addEdge(node, body, new Edge(GraphUtil.eid(), EdgeType.BODY)));

          return Optional.of(node);
        }
      case ASTNode.DO_STATEMENT:
        {
          DoStatement doStatement = ((DoStatement) stmt);
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(), Language.JAVA, NodeType.DO_STATEMENT, doStatement.toString());
          node.setRange(computeRange(stmt));

          graph.addVertex(node);

          Expression expression = doStatement.getExpression();
          if (expression != null) {
            RelationNode cond = parseExpression(expression);
            graph.addEdge(node, cond, new Edge(GraphUtil.eid(), EdgeType.CONDITION));
          }

          Statement doBody = doStatement.getBody();
          if (doBody != null) {
            parseStatement(doBody)
                .ifPresent(
                    body -> graph.addEdge(node, body, new Edge(GraphUtil.eid(), EdgeType.BODY)));
          }
          return Optional.of(node);
        }
      case ASTNode.WHILE_STATEMENT:
        {
          WhileStatement whileStatement = (WhileStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(),
                  Language.JAVA,
                  NodeType.WHILE_STATEMENT,
                  whileStatement.toString());
          node.setRange(computeRange(stmt));

          graph.addVertex(node);

          Expression expression = whileStatement.getExpression();
          if (expression != null) {
            RelationNode cond = parseExpression(expression);
            graph.addEdge(node, cond, new Edge(GraphUtil.eid(), EdgeType.CONDITION));
          }

          Statement whileBody = whileStatement.getBody();
          if (whileBody != null) {
            parseStatement(whileBody)
                .ifPresent(
                    body -> graph.addEdge(node, body, new Edge(GraphUtil.eid(), EdgeType.BODY)));
          }
          return Optional.of(node);
        }
      case ASTNode.TRY_STATEMENT:
        {
          TryStatement tryStatement = (TryStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(), Language.JAVA, NodeType.TRY_STATEMENT, tryStatement.toString());
          node.setRange(computeRange(stmt));

          graph.addVertex(node);

          Statement tryBody = tryStatement.getBody();
          if (tryBody != null) {
            parseStatement(tryBody)
                .ifPresent(
                    body -> graph.addEdge(node, body, new Edge(GraphUtil.eid(), EdgeType.BODY)));

            List<CatchClause> catchClauses = tryStatement.catchClauses();
            if (catchClauses != null && !catchClauses.isEmpty()) {
              for (CatchClause catchClause : catchClauses) {
                ITypeBinding binding = catchClause.getException().getType().resolveBinding();
                RelationNode catchNode =
                    new RelationNode(
                        GraphUtil.nid(),
                        Language.JAVA,
                        NodeType.CATCH_CLAUSE,
                        catchClause.toString());
                catchNode.setRange(computeRange(catchClause));

                graph.addVertex(catchNode);
                graph.addEdge(node, catchNode, new Edge(GraphUtil.eid(), EdgeType.CATCH));

                if (catchClause.getBody() != null) {
                  parseBodyBlock(catchClause.getBody())
                      .ifPresent(
                          block ->
                              graph.addEdge(
                                  catchNode, block, new Edge(GraphUtil.eid(), EdgeType.CHILD)));
                }
              }
            }
            if (tryStatement.getFinally() != null) {
              parseBodyBlock(tryStatement.getFinally())
                  .ifPresent(
                      block ->
                          graph.addEdge(node, block, new Edge(GraphUtil.eid(), EdgeType.FINALLY)));
            }
          }
          return Optional.of(node);
        }
      case ASTNode.THROW_STATEMENT:
        {
          ThrowStatement throwStatement = (ThrowStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(),
                  Language.JAVA,
                  NodeType.THROW_STATEMENT,
                  throwStatement.toString());
          node.setRange(computeRange(stmt));

          graph.addVertex(node);

          if (throwStatement.getExpression() != null) {
            RelationNode thr = parseExpression(throwStatement.getExpression());
            graph.addEdge(node, thr, new Edge(GraphUtil.eid(), EdgeType.THROW));
          }

          return Optional.of(node);
        }
      case ASTNode.SWITCH_STATEMENT:
        {
          SwitchStatement switchStatement = (SwitchStatement) stmt;
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(),
                  Language.JAVA,
                  NodeType.SWITCH_STATEMENT,
                  switchStatement.toString());
          node.setRange(computeRange(stmt));

          graph.addVertex(node);

          Expression expression = switchStatement.getExpression();
          if (expression != null) {
            RelationNode cond = parseExpression(expression);
            graph.addEdge(node, cond, new Edge(GraphUtil.eid(), EdgeType.CONDITION));
          }
          // treat case as an implicit block of statements
          for (int i = 0; i < switchStatement.statements().size(); ++i) {
            Object nxt = switchStatement.statements().get(i);
            if (nxt instanceof SwitchCase) {
              SwitchCase switchCase = (SwitchCase) nxt;
              RelationNode caseNode =
                  new RelationNode(
                      GraphUtil.nid(),
                      Language.JAVA,
                      NodeType.SWITCH_CASE,
                      ((SwitchCase) nxt).toString());
              caseNode.setRange(computeRange((SwitchCase) nxt));

              graph.addVertex(caseNode);
              for (Object exx : switchCase.expressions()) {
                if (exx instanceof Expression) {
                  RelationNode condition = parseExpression((Expression) exx);
                  graph.addVertex(condition);
                  graph.addEdge(caseNode, condition, new Edge(GraphUtil.eid(), EdgeType.CONDITION));
                }
              }
              graph.addEdge(node, caseNode, new Edge(GraphUtil.eid(), EdgeType.CHILD));

              while (i + 1 < switchStatement.statements().size()) {
                Object nxxt = switchStatement.statements().get(++i);
                if (nxxt instanceof BreakStatement) {
                  break;
                } else if (nxxt instanceof SwitchCase) {
                  i -= 1;
                  break;
                } else if (nxxt instanceof Statement) {
                  parseStatement((Statement) nxxt)
                      .ifPresent(
                          then ->
                              graph.addEdge(
                                  caseNode, then, new Edge(GraphUtil.eid(), EdgeType.THEN)));
                }
              }
            }
          }
          return Optional.of(node);
        }
    }

    return Optional.empty();
  }
}
