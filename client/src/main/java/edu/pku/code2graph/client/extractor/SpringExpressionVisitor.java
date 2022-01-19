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
  private List<URI> globalAttr = new ArrayList<>();

  public SpringExpressionVisitor(Map<String, List<URI>> uris) {
    super();
    javaURIS = uris;
  }

  public boolean visit(MethodDeclaration md) {
    currentTemplate = "";
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

    // return type
    if (mdBinding != null) {
      ITypeBinding tpBinding = mdBinding.getReturnType();
      if (tpBinding != null) {
        usePool.add(Triple.of(node, EdgeType.RETURN_TYPE, tpBinding.getQualifiedName()));
      }
    }

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
          defPool.put(para_qname, pn);
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
        parseBodyBlock(md.getBody(), md.getName().toString(), qname)
            .ifPresent(
                blockNode ->
                    graph.addEdge(node, blockNode, new Edge(GraphUtil.eid(), EdgeType.BODY)));
      }
    }

    currentTemplate = "";
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
                && pathRegex.matcher(returnExpr).find()) {
              currentTemplate = returnExpr.substring(1, returnExpr.length() - 1);
              return Optional.of(returnExpr);
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  private void parseAnnotations(List modifiers, ElementNode annotatedNode) {
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

        // check annotation type and possible refs
        // add possible refs into use pool
        ITypeBinding typeBinding = annotation.resolveTypeBinding();
        String typeQName =
            typeBinding == null
                ? annotation.getTypeName().getFullyQualifiedName()
                : typeBinding.getQualifiedName();
        if (annotation.isMarkerAnnotation()) {
          // @A
          usePool.add(Triple.of(node, EdgeType.REFERENCE, typeQName));
        } else if (annotation.isSingleMemberAnnotation()) {
          // @A(v)
          Expression value = ((SingleMemberAnnotation) annotation).getValue();
          Expression typeName = annotation.getTypeName();
          if (typeName.toString().equals("ModelAttribute")) {
            URI uri = new URI(true, uriFilePath);
            uri.addLayer(modifier.toString(), Language.JAVA);
            globalAttr.add(uri);
          }
          usePool.add(Triple.of(node, EdgeType.REFERENCE, typeQName));
          usePool.add(Triple.of(node, EdgeType.REFERENCE, value.toString()));
        } else if (annotation.isNormalAnnotation()) {
          // @A(k=v)
          usePool.add(Triple.of(node, EdgeType.REFERENCE, typeQName));

          for (Object value : ((NormalAnnotation) annotation).values()) {
            if (value instanceof MemberValuePair) {
              MemberValuePair pair = ((MemberValuePair) value);
              ITypeBinding binding = pair.getValue().resolveTypeBinding();
              String qname =
                  binding == null ? pair.getValue().toString() : binding.getQualifiedName();
              usePool.add(Triple.of(node, EdgeType.REFERENCE, qname));
            }
          }
        }
      }
    }
  }

  /**
   * Process arguments of (super) method/constructor invocation
   *
   * @param node
   * @param arguments
   */
  private void parseArguments(RelationNode node, List arguments, boolean addedToMap) {
    for (Object arg : arguments) {
      if (arg instanceof Expression) {
        RelationNode rn = parseExpression((Expression) arg);
        graph.addEdge(node, rn, new Edge(GraphUtil.eid(), EdgeType.ARGUMENT));
        if (rn.getUri() == null) continue;
        if (currentTemplate != "" && addedToMap && !javaURIS.containsKey(currentTemplate)) {
          javaURIS.put(currentTemplate, new ArrayList<>());
          javaURIS.get(currentTemplate).add(rn.getUri());
        } else if (currentTemplate != "" && addedToMap) {
          if (!javaURIS.get(currentTemplate).contains(rn.getUri()))
            javaURIS.get(currentTemplate).add(rn.getUri());
        }
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
          URI uri = createIdentifier(identifier, false);
          root.setUri(uri);
          GraphUtil.addNode(root);
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
            usePool.add(
                Triple.of(root, EdgeType.REFERENCE, identifier));
          } else if (binding instanceof IVariableBinding) {
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
                      JdtService.getVariableQNameFromBinding(varBinding, exp)));
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
                      JdtService.getVariableQNameFromBinding(varBinding, exp)));
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

            if (binding != null) {
              usePool.add(Triple.of(n, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
            }
          }
          break;
        }
      case ASTNode.SUPER_FIELD_ACCESS:
        {
          SuperFieldAccess fa = (SuperFieldAccess) exp;
          root.setType(NodeType.SUPER_FIELD_ACCESS);
          root.setUri(createIdentifier(fa.toString()));

          IVariableBinding faBinding = fa.resolveFieldBinding();
          if (faBinding != null && faBinding.isField() && faBinding.getDeclaringClass() != null) {
            usePool.add(
                Triple.of(
                    root,
                    EdgeType.REFERENCE,
                    faBinding.getDeclaringClass().getQualifiedName() + "." + faBinding.getName()));
          }
          break;
        }
      case ASTNode.FIELD_ACCESS:
        {
          root.setType(NodeType.FIELD_ACCESS);
          FieldAccess fa = (FieldAccess) exp;

          IVariableBinding faBinding = fa.resolveFieldBinding();
          if (faBinding != null && faBinding.isField() && faBinding.getDeclaringClass() != null) {
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

          ClassInstanceCreation cic = (ClassInstanceCreation) exp;
          IMethodBinding constructorBinding = cic.resolveConstructorBinding();
          if (constructorBinding != null) {
            usePool.add(
                Triple.of(
                    root,
                    EdgeType.DATA_TYPE,
                    constructorBinding.getDeclaringClass().getQualifiedName()));
          }
//          parseArguments(root, cic.arguments(), false);
          String identifier = cic.getType().toString();
          withScope(identifier, () -> parseArguments(root, cic.arguments()));
          break;
        }
      case ASTNode.SUPER_METHOD_INVOCATION:
        {
          SuperMethodInvocation mi = (SuperMethodInvocation) exp;
          String identifier = "super." + mi.getName().toString();
          root.setType(NodeType.SUPER_METHOD_INVOCATION);
          root.setUri(createIdentifier(identifier));

//          parseArguments(root, mi.arguments(), false);
          withScope(identifier, () -> parseArguments(root, mi.arguments()));
          // find the method declaration in super class
          IMethodBinding mdBinding = mi.resolveMethodBinding();
          if (mdBinding != null) {
            // get caller qname
            JdtService.findWrappedMethodName(mi)
                .ifPresent(
                    name -> {
                      usePool.add(Triple.of(root, EdgeType.CALLER, name));
                    });
            // get callee qname
            usePool.add(
                Triple.of(root, EdgeType.CALLEE, JdtService.getMethodQNameFromBinding(mdBinding)));
          }
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

          if (exp.toString().contains("addAttribute") || exp.toString().contains("setAttribute")) {
            //          parseArguments(root, mi.arguments(), addedToMap);
            String identifier = root.getUri().getIdentifier();
            String arg = mi.arguments().get(0).toString();
            URI uri = new URI(false, uriFilePath);
            uri.addLayer(identifier, Language.JAVA);
            uri.addLayer(arg.substring(1, arg.length() - 1), Language.SQL);

            if (currentTemplate != "" && !javaURIS.containsKey(currentTemplate)) {
              javaURIS.put(currentTemplate, new ArrayList<>());
              javaURIS.get(currentTemplate).add(uri);
            } else if (currentTemplate != "") {
              if (!javaURIS.get(currentTemplate).contains(uri))
                javaURIS.get(currentTemplate).add(uri);
            }
          }

          IMethodBinding mdBinding = mi.resolveMethodBinding();
          // only internal invocation (or consider types, fields and local?)
          if (mdBinding != null) {
            // get caller qname
            JdtService.findWrappedMethodName(mi)
                .ifPresent(
                    name -> {
                      usePool.add(Triple.of(root, EdgeType.CALLER, name));
                    });
            // get callee qname
            usePool.add(
                Triple.of(root, EdgeType.CALLEE, JdtService.getMethodQNameFromBinding(mdBinding)));
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

          ITypeBinding typeBinding = castExpression.getType().resolveBinding();
          if (typeBinding != null) {
            usePool.add(Triple.of(root, EdgeType.TARGET_TYPE, typeBinding.getQualifiedName()));
          }

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
}
