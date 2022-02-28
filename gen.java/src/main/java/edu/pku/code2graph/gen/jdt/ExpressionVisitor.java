package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.gen.sql.JsqlGenerator;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.jdt.core.dom.*;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;

import java.util.*;
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
public class ExpressionVisitor extends AbstractJdtVisitor {
  private ElementNode cuNode;

  private List<String> annotationList = Arrays.asList("Select", "Update", "Insert", "Delete");

  private JsqlGenerator generator = new JsqlGenerator();

  @Override
  public boolean visit(CompilationUnit cu) {
    this.cuNode =
        createElementNode(NodeType.FILE, "", FileUtil.getFileNameFromPath(filePath), filePath, "");

    logger.debug("Start Parsing {}", uriFilePath);
    return true;
  }

  @Override
  public void preVisit(ASTNode n) {}

  public boolean visit(PackageDeclaration pd) {
    return true;
  }

  public boolean visit(TypeDeclaration td) {
    Type type = td.isInterface() ? NodeType.INTERFACE_DECLARATION : NodeType.CLASS_DECLARATION;
    ITypeBinding tdBinding = td.resolveBinding();
    String qname =
        tdBinding == null
            ? JdtService.getTypeQNameFromParents(td)
            : td.getName().getFullyQualifiedName();

    ElementNode node =
        createElementNode(
            type, td.toString(), td.getName().toString(), qname, JdtService.getIdentifier(td));

    node.setRange(computeRange(td));

    if (tdBinding != null) {
      if (tdBinding.isTopLevel()) {
        graph.addEdge(cuNode, node, new Edge(GraphUtil.eid(), EdgeType.CHILD));
      }
      parseAnnotations(td.modifiers(), node);
      parseExtendsAndImplements(tdBinding, node);
      parseMembers(td, tdBinding, node);
    } else {
      if (td.isPackageMemberTypeDeclaration()) {
        graph.addEdge(cuNode, node, new Edge(GraphUtil.eid(), EdgeType.CHILD));
      }
    }

    return true;
  }

  public boolean visit(AnnotationTypeDeclaration atd) {
    ITypeBinding binding = atd.resolveBinding();
    String qname =
        binding == null ? atd.getName().getFullyQualifiedName() : binding.getQualifiedName();
    ElementNode node =
        createElementNode(
            NodeType.ANNOTATION_DECLARATION,
            atd.toString(),
            atd.getName().toString(),
            qname,
            JdtService.getIdentifier(atd));
    node.setRange(computeRange(atd));

    if (binding != null) {
      if (binding.isTopLevel()) {
        graph.addEdge(cuNode, node, new Edge(GraphUtil.eid(), EdgeType.CHILD));
      }
      parseAnnotations(atd.modifiers(), node);
      parseExtendsAndImplements(binding, node);
      parseMembers(atd, binding, node);
    } else {
      if (atd.isPackageMemberTypeDeclaration()) {
        graph.addEdge(cuNode, node, new Edge(GraphUtil.eid(), EdgeType.CHILD));
      }
    }

    return true;
  }

  public boolean visit(EnumDeclaration ed) {
    ITypeBinding edBinding = ed.resolveBinding();
    String qname =
        edBinding == null ? ed.getName().getFullyQualifiedName() : edBinding.getQualifiedName();
    ElementNode node =
        createElementNode(
            NodeType.ENUM_DECLARATION,
            ed.toString(),
            ed.getName().toString(),
            qname,
            JdtService.getIdentifier(ed));

    node.setRange(computeRange(ed));

    parseAnnotations(ed.modifiers(), node);

    if (edBinding != null) {
      if (edBinding.isTopLevel()) {
        graph.addEdge(cuNode, node, new Edge(GraphUtil.eid(), EdgeType.CHILD));
      }
      parseExtendsAndImplements(edBinding, node);
      parseMembers(ed, edBinding, node);
    } else {
      if (ed.isPackageMemberTypeDeclaration()) {
        graph.addEdge(cuNode, node, new Edge(GraphUtil.eid(), EdgeType.CHILD));
      }
    }

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
    if (parentType != null) {
      usePool.add(Triple.of(node, EdgeType.EXTENDED_CLASS, parentType.getQualifiedName()));
    }
    ITypeBinding[] interfaces = binding.getInterfaces();
    if (interfaces != null && interfaces.length > 0) {
      for (ITypeBinding anInterface : interfaces) {
        usePool.add(
            Triple.of(node, EdgeType.IMPLEMENTED_INTERFACE, anInterface.getQualifiedName()));
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
      AbstractTypeDeclaration typeDeclaration, ITypeBinding binding, ElementNode node) {
    List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
    if (bodyDeclarations.isEmpty()) {
      return;
    }
    String parentQName = node.getQualifiedName();
    withScope(parentQName, () -> {
      RelationNode bodyNode = new RelationNode(GraphUtil.nid(), Language.JAVA, NodeType.BLOCK, "{}");
      bodyNode.setRange(computeRange(typeDeclaration.bodyDeclarations()));

      graph.addVertex(bodyNode);
      defPool.put(parentQName + ".BLOCK", bodyNode);
      graph.addEdge(node, bodyNode, new Edge(GraphUtil.eid(), EdgeType.BODY));

      // annotation member
      if (typeDeclaration instanceof AnnotationTypeDeclaration) {
        for (Object bodyDeclaration : bodyDeclarations) {
          if (bodyDeclaration instanceof AnnotationTypeMemberDeclaration) {
            AnnotationTypeMemberDeclaration atmd =
                    ((AnnotationTypeMemberDeclaration) bodyDeclaration);
            ElementNode atmdNode =
                    createElementNode(
                            NodeType.ENUM_CONSTANT_DECLARATION,
                            atmd.toString(),
                            atmd.getName().toString(),
                            parentQName + "." + atmd.getName(),
                            JdtService.getIdentifier(atmd));

            atmdNode.setRange(computeRange(atmd));
            graph.addEdge(bodyNode, atmdNode, new Edge(GraphUtil.eid(), EdgeType.CHILD));
          }
        }
      }

      // enum constant member
      if (typeDeclaration instanceof EnumDeclaration) {
        for (Object constantDeclaration : ((EnumDeclaration) typeDeclaration).enumConstants()) {
          if (constantDeclaration instanceof EnumConstantDeclaration) {
            EnumConstantDeclaration cst = (EnumConstantDeclaration) constantDeclaration;
            ElementNode cstNode =
                    createElementNode(
                            NodeType.ENUM_CONSTANT_DECLARATION,
                            cst.toString(),
                            cst.getName().toString(),
                            parentQName + "." + cst.getName().toString(),
                            JdtService.getIdentifier(cst));

            cstNode.setRange(computeRange(cst));
            graph.addEdge(bodyNode, cstNode, new Edge(GraphUtil.eid(), EdgeType.CHILD));
          }
        }
      }

      // initializer
      List<Initializer> initializers =
              bodyDeclarations.stream()
                      .filter(Initializer.class::isInstance)
                      .map(Initializer.class::cast)
                      .collect(Collectors.toList());
      if (!initializers.isEmpty()) {
        for (Initializer initializer : initializers) {
          if (!initializer.getBody().statements().isEmpty()) {
            String qname = parentQName + ".INIT";

            ElementNode initNode =
                    createElementNode(
                            NodeType.INIT_BLOCK_DECLARATION,
                            initializer.toString(),
                            node.getName() + ".INIT",
                            qname,
                            JdtService.getIdentifier(initializer));

            initNode.setRange(computeRange(initializer));

            graph.addEdge(bodyNode, initNode, new Edge(GraphUtil.eid(), EdgeType.CHILD));
            defPool.put(qname, node);

            parseBodyBlock(initializer.getBody(), parentQName, parentQName)
                    .ifPresent(
                            initBlock ->
                                    graph.addEdge(
                                            initNode, initBlock, new Edge(GraphUtil.eid(), EdgeType.CHILD)));
          }
        }
      }

      // field
      IVariableBinding[] fdBindings = binding.getDeclaredFields();
      for (IVariableBinding b : fdBindings) {
        usePool.add(Triple.of(bodyNode, EdgeType.CHILD, parentQName + "." + b.getName()));
      }

      // method
      IMethodBinding[] mdBindings = binding.getDeclaredMethods();
      for (IMethodBinding b : mdBindings) {
        if (b.isDefaultConstructor()) {
          continue;
        }
        usePool.add(Triple.of(bodyNode, EdgeType.CHILD, JdtService.getMethodQNameFromBinding(b)));
      }
      return null;
    });
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
          createElementNode(
              NodeType.FIELD_DECLARATION,
              fragment.toString(),
              name,
              qname,
              JdtService.getIdentifier(fragment));

      node.setRange(computeRange(fragment));
      node.getUri().getLayer(1).addAttribute("varType", fd.getType().toString());

      // annotations
      parseAnnotations(fd.modifiers(), node);

      if (binding != null) {
        usePool.add(Triple.of(node, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
      }

      if (fragment.getInitializer() != null) {
        graph.addEdge(
            node,
            parseExpression(fragment.getInitializer()),
            new Edge(GraphUtil.eid(), EdgeType.INITIALIZER));
      }
    }
    return false;
  }

  public boolean visit(MethodDeclaration md) {
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
        if (p.toString().trim().startsWith("@Param")
            || p.toString().trim().startsWith("@ModelAttribute")) {
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

          URI uri = new URI(false, uriFilePath);
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

        pn.getUri().getLayer(1).addAttribute("varType", p.getType().toString());

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
    return true;
  }

  /**
   * Parse the body of element node, e.g. method/constructor, initializer block, and nested block
   *
   * @param body
   * @return
   */
  protected Optional<RelationNode> parseBodyBlock(Block body, String name, String rootName) {
    if (body == null || body.statements().isEmpty()) {
      return Optional.empty();
    }

    // the node of the current block node
    Optional<RelationNode> rootOpt = withScope(name, () -> parseBodyBlock(body));
    if (rootOpt.isPresent()) {
      defPool.put(rootName + ".BLOCK", rootOpt.get());
      return rootOpt;
    } else {
      return Optional.empty();
    }
  }

  protected void parseAnnotations(List modifiers, ElementNode annotatedNode) {
    for (Object modifier : modifiers) {
      if (!(modifier instanceof Annotation)) continue;
      Annotation annotation = (Annotation) modifier;
      // create node as a child of annotatedNode

      String query = null, idtf = null, filepath = null;
      Language lang = null;
      if (annotation.getTypeName() instanceof SimpleName) {
        String anTypeSimpleName = ((SimpleName) annotation.getTypeName()).getIdentifier();
        if (annotationList.contains(anTypeSimpleName)
                && annotation instanceof SingleMemberAnnotation) {
          query = ((SingleMemberAnnotation) annotation).getValue().toString();
          query = query.substring(1, query.length() - 1);
          if (annotatedNode.getUri() == null) {
            query = null;
          } else {
            idtf = annotatedNode.getUri().getIdentifier() + "/" + anTypeSimpleName;
            lang = annotatedNode.getLanguage();
            filepath = annotatedNode.getUri().getFile();
          }
        }
      }

      if (query != null) {
        query = StringEscapeUtils.unescapeJava(query);
        Graph<Node, Edge> graph =
            generator.generate(query, FileUtil.getRootPath() + "/" + filepath, lang, idtf, "");

        Map<String, List<RelationNode>> queryList = generator.getQueries();
        if (annotatedNode != null && queryList.get("") != null) {
          for (RelationNode rn : queryList.get("")) {
            graph.addEdge(annotatedNode, rn, new Edge(GraphUtil.eid(), EdgeType.INLINE_SQL));
          }
        }

        generator.clearQueries();
        generator.clearIdentifiers();
      }

      identifier = annotation.getTypeName().getFullyQualifiedName();
      RelationNode node =
          new RelationNode(
              GraphUtil.nid(),
              Language.JAVA,
              NodeType.ANNOTATION,
              annotation.toString(),
              identifier);
      node.setRange(computeRange(annotation));
      node.setUri(createIdentifier(identifier));
      GraphUtil.addNode(node);

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
        usePool.add(Triple.of(node, EdgeType.REFERENCE, typeQName));
        usePool.add(Triple.of(node, EdgeType.REFERENCE, value.toString()));
        parseExpression(value);
      } else if (annotation.isNormalAnnotation()) {
        // @A(k=v)
        usePool.add(Triple.of(node, EdgeType.REFERENCE, typeQName));

        for (Object _pair : ((NormalAnnotation) annotation).values()) {
          if (!(_pair instanceof  MemberValuePair)) continue;
          MemberValuePair pair = (MemberValuePair) _pair;
          Expression innerValue = pair.getValue();
          ITypeBinding binding = innerValue.resolveTypeBinding();
          String qname =
              binding == null ? innerValue.toString() : binding.getQualifiedName();
          usePool.add(Triple.of(node, EdgeType.REFERENCE, qname));
          parseExpression(innerValue);
        }
      }
    }
  }

  /**
   * Parse the body of relation node, e.g. finally, catch
   *
   * @param body
   * @return
   */
  protected Optional<RelationNode> parseBodyBlock(Block body) {
    RelationNode root = new RelationNode(GraphUtil.nid(), Language.JAVA, NodeType.BLOCK, "{}");
    root.setRange(computeRange(body));
    graph.addVertex(root);

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
          .ifPresent(node -> graph.addEdge(root, node, new Edge(GraphUtil.eid(), EdgeType.CHILD)));
    }
    return Optional.of(root);
  }

  /**
   * Process arguments of (super) method/constructor invocation
   *
   * @param node
   * @param arguments
   */
  protected RelationNode parseArguments(RelationNode node, List arguments) {
    for (Object arg : arguments) {
      if (arg instanceof Expression) {
        graph.addEdge(
            node, parseExpression((Expression) arg), new Edge(GraphUtil.eid(), EdgeType.ARGUMENT));
      }
    }
    return node;
  }

  /**
   * Parse statement and return the created node
   *
   * @param stmt
   * @return
   */
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
          IMethodBinding constructorBinding = ci.resolveConstructorBinding();
          if (constructorBinding != null) {
            usePool.add(
                Triple.of(
                    node,
                    EdgeType.REFERENCE,
                    constructorBinding.getDeclaringClass().getQualifiedName()));
          }
          withScope("super", () -> parseArguments(node, ci.arguments()));
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
          IMethodBinding constructorBinding = ci.resolveConstructorBinding();
          if (constructorBinding != null) {
            usePool.add(
                Triple.of(
                    node,
                    EdgeType.REFERENCE,
                    constructorBinding.getDeclaringClass().getQualifiedName()));
          }
          withScope("this", () -> parseArguments(node, ci.arguments()));
          return Optional.of(node);
        }
      case ASTNode.RETURN_STATEMENT:
        {
          Expression expression = ((ReturnStatement) stmt).getExpression();
          if (expression != null) {
            RelationNode node = withScope("return", () -> parseExpression(expression));
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
              qname = JdtService.getVariableQNameFromBinding(binding, stmt);

              ElementNode node =
                  createElementNode(
                      NodeType.VAR_DECLARATION,
                      fragment.toString(),
                      name,
                      qname,
                      JdtService.getIdentifier(fragment));

              node.setRange(computeRange(fragment));

              usePool.add(
                  Triple.of(node, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));

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

          ITypeBinding paraBinding = p.getType().resolveBinding();
          if (paraBinding != null) {
            usePool.add(Triple.of(pn, EdgeType.DATA_TYPE, paraBinding.getQualifiedName()));
          }

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

                if (binding != null) {
                  usePool.add(Triple.of(node, EdgeType.TARGET_TYPE, binding.getQualifiedName()));
                }
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
          String content = URI.checkInvalidCh(((StringLiteral) exp).getLiteralValue());
          URI uri = createIdentifier(null, false);
          uri.addLayer(content);
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

          withScope(identifier, () -> parseArguments(root, mi.arguments()));

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
