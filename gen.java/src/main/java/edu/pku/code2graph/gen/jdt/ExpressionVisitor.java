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
  public void preVisit(ASTNode node) {
    if (node instanceof TypeDeclaration) {
      pushScope(((TypeDeclaration) node).getName().toString());
    }
  }

  @Override
  public void postVisit(ASTNode node) {
    if (node instanceof TypeDeclaration) {
      popScope();
    }
  }

  @Override
  public boolean visit(CompilationUnit cu) {
    URI uri = new URI(false, uriFilePath);
    this.cuNode =
        new ElementNode(
            GraphUtil.nid(),
            Language.JAVA,
            NodeType.FILE,
            "",
            FileUtil.getFileNameFromPath(filePath),
            filePath,
            uri);
    GraphUtil.addNode(cuNode);
    logger.debug("Start Parsing {}", uriFilePath);
    return true;
  }

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
      parseAnnotations(td.modifiers(), node);
      parseExtendsAndImplements(tdBinding, node);
      parseMembers(td, tdBinding, node);
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
      parseAnnotations(atd.modifiers(), node);
      parseExtendsAndImplements(binding, node);
      parseMembers(atd, binding, node);
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
      parseExtendsAndImplements(edBinding, node);
      parseMembers(ed, edBinding, node);
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
    pushScope(parentQName);
    RelationNode bodyNode = new RelationNode(GraphUtil.nid(), Language.JAVA, NodeType.BLOCK, "{}");
    bodyNode.setRange(computeRange(typeDeclaration.bodyDeclarations()));

    defPool.put(parentQName + ".BLOCK", bodyNode);

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

          defPool.put(qname, node);

          parseBodyBlock(initializer.getBody(), parentQName, parentQName);
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

    popScope();
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
              JdtService.getIdentifier(fragment),
              (layer) -> {
                layer.put("varType", fd.getType().toString());
              });

      node.setRange(computeRange(fragment));

      // annotations
      pushScope(name);
      parseAnnotations(fd.modifiers(), node);
      popScope();

      if (binding != null) {
        usePool.add(Triple.of(node, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
      }

      if (fragment.getInitializer() != null) {
        parseExpression(fragment.getInitializer());
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

    pushScope(name);

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
                JdtService.getIdentifier(p),
                (layer) -> {
                  layer.put("varType", p.getType().toString());
                });

        pn.setRange(computeRange(p));

        pushScope(para_name);
        parseAnnotations(p.modifiers(), node);
        popScope();

        ITypeBinding paraBinding = p.getType().resolveBinding();
        if (paraBinding != null) {
          usePool.add(Triple.of(pn, EdgeType.DATA_TYPE, paraBinding.getQualifiedName()));
        }
      }
    }

    // TODO: add exception types
    if (!md.thrownExceptionTypes().isEmpty()) {}

    popScope();

    parseBodyBlock(md.getBody(), md.getName().toString(), qname);
    return true;
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
            generator.generate(
                query,
                FileUtil.getRootPath() + "/" + filepath,
                lang,
                idtf,
                annotatedNode.getUri(),
                idtf);

        List<ElementNode> identifierById = generator.getIdentifiers().get(idtf);
        for (ElementNode node : identifierById) {
          GraphUtil.addNode(node);
        }

        // TODO: check the concurrency
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
      node.setUri(createIdentifier("@" + identifier));
      GraphUtil.addNode(node);

      pushScope("@" + identifier);

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
          if (!(_pair instanceof MemberValuePair)) continue;
          MemberValuePair pair = (MemberValuePair) _pair;
          Expression innerValue = pair.getValue();
          ITypeBinding binding = innerValue.resolveTypeBinding();
          String qname = binding == null ? innerValue.toString() : binding.getQualifiedName();
          usePool.add(Triple.of(node, EdgeType.REFERENCE, qname));
          parseExpression(innerValue);
        }
      }

      popScope();
    }
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
    pushScope(name);
    Optional<RelationNode> rootOpt = parseBodyBlock(body);
    popScope();
    if (rootOpt.isPresent()) {
      defPool.put(rootName + ".BLOCK", rootOpt.get());
      return rootOpt;
    } else {
      return Optional.empty();
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

      parseStatement(stmt);
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
        parseExpression((Expression) arg);
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

            for (Object st : block.statements()) {
              parseStatement((Statement) st);
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

          SuperConstructorInvocation ci = (SuperConstructorInvocation) stmt;
          IMethodBinding constructorBinding = ci.resolveConstructorBinding();
          if (constructorBinding != null) {
            usePool.add(
                Triple.of(
                    node,
                    EdgeType.REFERENCE,
                    constructorBinding.getDeclaringClass().getQualifiedName()));
          }
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

          ConstructorInvocation ci = (ConstructorInvocation) stmt;
          IMethodBinding constructorBinding = ci.resolveConstructorBinding();
          if (constructorBinding != null) {
            usePool.add(
                Triple.of(
                    node,
                    EdgeType.REFERENCE,
                    constructorBinding.getDeclaringClass().getQualifiedName()));
          }
          pushScope("this");
          parseArguments(node, ci.arguments());
          popScope();
          return Optional.of(node);
        }
      case ASTNode.RETURN_STATEMENT:
        {
          Expression expression = ((ReturnStatement) stmt).getExpression();
          if (expression != null) {
            pushScope("return");
            RelationNode node = parseExpression(expression);
            popScope();
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
                      JdtService.getIdentifier(fragment),
                      (layer) -> {
                        layer.put("varType", vd.getType().toString());
                      });

              node.setRange(computeRange(fragment));

              usePool.add(
                  Triple.of(node, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));

              if (fragment.getInitializer() != null) {
                parseExpression(fragment.getInitializer());
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

          if (ifStatement.getExpression() != null) {
            RelationNode cond = parseExpression(ifStatement.getExpression());
          }
          if (ifStatement.getThenStatement() != null) {
            parseStatement(ifStatement.getThenStatement());
          }
          if (ifStatement.getElseStatement() != null) {
            parseStatement(ifStatement.getElseStatement());
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

          forStatement.initializers().forEach(init -> parseExpression((Expression) init));
          forStatement.updaters().forEach(upd -> parseExpression((Expression) upd));

          if (forStatement.getExpression() != null) {
            parseExpression(forStatement.getExpression());
          }
          parseStatement(forStatement.getBody());

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

          ITypeBinding paraBinding = p.getType().resolveBinding();
          if (paraBinding != null) {
            usePool.add(Triple.of(pn, EdgeType.DATA_TYPE, paraBinding.getQualifiedName()));
          }

          parseExpression(eForStatement.getExpression());
          parseStatement(eForStatement.getBody());

          return Optional.of(node);
        }
      case ASTNode.DO_STATEMENT:
        {
          DoStatement doStatement = ((DoStatement) stmt);
          RelationNode node =
              new RelationNode(
                  GraphUtil.nid(), Language.JAVA, NodeType.DO_STATEMENT, doStatement.toString());
          node.setRange(computeRange(stmt));

          Expression expression = doStatement.getExpression();
          if (expression != null) {
            parseExpression(expression);
          }

          Statement doBody = doStatement.getBody();
          if (doBody != null) {
            parseStatement(doBody);
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

          Expression expression = whileStatement.getExpression();
          if (expression != null) {
            RelationNode cond = parseExpression(expression);
          }

          Statement whileBody = whileStatement.getBody();
          if (whileBody != null) {
            parseStatement(whileBody);
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

          Statement tryBody = tryStatement.getBody();
          if (tryBody != null) {
            parseStatement(tryBody);

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

                if (binding != null) {
                  usePool.add(Triple.of(node, EdgeType.TARGET_TYPE, binding.getQualifiedName()));
                }
                if (catchClause.getBody() != null) {
                  parseBodyBlock(catchClause.getBody());
                }
              }
            }
            if (tryStatement.getFinally() != null) {
              parseBodyBlock(tryStatement.getFinally());
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

          if (throwStatement.getExpression() != null) {
            RelationNode thr = parseExpression(throwStatement.getExpression());
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

          Expression expression = switchStatement.getExpression();
          if (expression != null) {
            parseExpression(expression);
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

              for (Object exx : switchCase.expressions()) {
                if (exx instanceof Expression) {
                  parseExpression((Expression) exx);
                }
              }

              while (i + 1 < switchStatement.statements().size()) {
                Object nxxt = switchStatement.statements().get(++i);
                if (nxxt instanceof BreakStatement) {
                  break;
                } else if (nxxt instanceof SwitchCase) {
                  i -= 1;
                  break;
                } else if (nxxt instanceof Statement) {
                  parseStatement((Statement) nxxt);
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
            usePool.add(Triple.of(root, EdgeType.REFERENCE, identifier));
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

          pushScope(identifier);
          parseArguments(root, mi.arguments());
          popScope();
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
            parseExpression(expr);
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
          URI uri = root.getUri();
          uri.getLayer(uri.getLayerCount()-1).put("isFunc", "true");
          GraphUtil.addNode(root);

          pushScope(identifier);
          parseArguments(root, mi.arguments());
          popScope();

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

          parseExpression(asg.getLeftHandSide());
          parseExpression(asg.getRightHandSide());
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

          parseExpression(castExpression.getExpression());

          break;
        }
      case ASTNode.INFIX_EXPRESSION:
        {
          InfixExpression iex = (InfixExpression) exp;
          root.setType(NodeType.INFIX);
          root.setSymbol(iex.getOperator().toString());
          root.setArity(2);

          parseExpression(iex.getLeftOperand());
          parseExpression(iex.getRightOperand());
          break;
        }
      case ASTNode.PREFIX_EXPRESSION:
        {
          PrefixExpression pex = (PrefixExpression) exp;
          root.setType(NodeType.PREFIX);
          root.setSymbol(pex.getOperator().toString());
          root.setArity(1);

          parseExpression(pex.getOperand());
          break;
        }
      case ASTNode.POSTFIX_EXPRESSION:
        {
          PostfixExpression pex = (PostfixExpression) exp;
          root.setType(NodeType.POSTFIX);
          root.setSymbol(pex.getOperator().toString());
          root.setArity(1);

          parseExpression(pex.getOperand());
        }
    }

    return root;
  }
}
