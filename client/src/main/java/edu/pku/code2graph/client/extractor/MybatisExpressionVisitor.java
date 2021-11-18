package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.gen.jdt.AbstractJdtVisitor;
import edu.pku.code2graph.gen.jdt.JdtService;
import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.gen.sql.JsqlGenerator;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.jdt.core.dom.*;
import org.jgrapht.Graph;
import org.apache.commons.lang3.tuple.Pair;
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
public class MybatisExpressionVisitor extends AbstractJdtVisitor {
  private ElementNode cuNode;

  private Map<String, Map<String, List<MybatisParam>>> paramMap;
  private Map<String, Map<String, URI>> fieldMap;
  private List<Pair<URI, URI>> uriPairs;

  private List<String> annotationList = Arrays.asList("Select", "Update", "Insert", "Delete");

  JsqlGenerator generator = new JsqlGenerator();

  public MybatisExpressionVisitor(
      Map<String, Map<String, List<MybatisParam>>> param,
      Map<String, Map<String, URI>> field,
      List<Pair<URI, URI>> pairs) {
    paramMap = param;
    fieldMap = field;
    uriPairs = pairs;
  }

  @Override
  public boolean visit(CompilationUnit cu) {
    this.cuNode =
        createElementNode(NodeType.FILE, "", FileUtil.getFileNameFromPath(filePath), filePath, "");

    logger.debug("Start Parsing {}", filePath);
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
      parseAnnotations(td.modifiers(), node, null);
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
      parseAnnotations(atd.modifiers(), node, null);
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

    parseAnnotations(ed.modifiers(), node, null);

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
    RelationNode bodyNode = new RelationNode(GraphUtil.nid(), Language.JAVA, NodeType.BLOCK, "{}");
    bodyNode.setRange(computeRange(typeDeclaration.bodyDeclarations()));

    defPool.put(parentQName + ".BLOCK", bodyNode);

    // annotation member
    //    if (typeDeclaration instanceof AnnotationTypeDeclaration) {
    //      for (Object bodyDeclaration : bodyDeclarations) {
    //        if (bodyDeclaration instanceof AnnotationTypeMemberDeclaration) {
    //          AnnotationTypeMemberDeclaration atmd =
    //              ((AnnotationTypeMemberDeclaration) bodyDeclaration);
    //          ElementNode atmdNode =
    //              createElementNode(
    //                  NodeType.ENUM_CONSTANT_DECLARATION,
    //                  atmd.toString(),
    //                  atmd.getName().toString(),
    //                  parentQName + "." + atmd.getName(),
    //                  JdtService.getIdentifier(atmd));
    //
    //          atmdNode.setRange(computeRange(atmd));
    //          graph.addEdge(bodyNode, atmdNode, new Edge(GraphUtil.eid(), EdgeType.CHILD));
    //        }
    //      }
    //    }

    // enum constant member
    //    if (typeDeclaration instanceof EnumDeclaration) {
    //      for (Object constantDeclaration : ((EnumDeclaration) typeDeclaration).enumConstants()) {
    //        if (constantDeclaration instanceof EnumConstantDeclaration) {
    //          EnumConstantDeclaration cst = (EnumConstantDeclaration) constantDeclaration;
    //          ElementNode cstNode =
    //              createElementNode(
    //                  NodeType.ENUM_CONSTANT_DECLARATION,
    //                  cst.toString(),
    //                  cst.getName().toString(),
    //                  parentQName + "." + cst.getName().toString(),
    //                  JdtService.getIdentifier(cst));
    //
    //          cstNode.setRange(computeRange(cst));
    //          graph.addEdge(bodyNode, cstNode, new Edge(GraphUtil.eid(), EdgeType.CHILD));
    //        }
    //      }
    //    }

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

          parseBodyBlock(initializer.getBody(), parentQName + ".BLOCK");
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
  }

  public boolean visit(FieldDeclaration fd) {
    List<VariableDeclarationFragment> fragments = fd.fragments();
    for (VariableDeclarationFragment fragment : fragments) {
      String name = fragment.getName().getFullyQualifiedName();
      String qname = name;
      IVariableBinding binding = fragment.resolveBinding();
      String packageName = null;
      if (binding != null && binding.getDeclaringClass() != null) {
        packageName = binding.getDeclaringClass().getQualifiedName();
        qname = packageName + "." + name;
        if (!fieldMap.containsKey(packageName)) fieldMap.put(packageName, new HashMap<>());
      }

      ElementNode node =
          createElementNode(
              NodeType.FIELD_DECLARATION,
              fragment.toString(),
              name,
              qname,
              JdtService.getIdentifier(fragment));

      if (packageName != null) {
        fieldMap.get(packageName).put(name, node.getUri());
      }

      node.setRange(computeRange(fragment));

      // annotations
      parseAnnotations(fd.modifiers(), node, null);

      if (binding != null) {
        usePool.add(Triple.of(node, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
      }
    }
    return false;
  }

  public boolean visit(MethodDeclaration md) {
    // A binding represents a named entity in the Java language
    // for internal, should never be null
    IMethodBinding mdBinding = md.resolveBinding();
    // can be null for method in local anonymous class
    String declaringClass = null;
    if (mdBinding == null) {
      return true;
    }
    if (mdBinding.getDeclaringClass() != null) {
      declaringClass = mdBinding.getDeclaringClass().getQualifiedName();
    }

    String name = md.getName().getFullyQualifiedName();
    String qname = name;
    if (mdBinding != null) {
      qname = JdtService.getMethodQNameFromBinding(mdBinding);
    }

    ElementNode node =
        createElementNode(
            NodeType.METHOD_DECLARATION, md.toString(), name, qname, JdtService.getIdentifier(md));

    node.setRange(computeRange(md));

    // return type
    if (mdBinding != null) {
      ITypeBinding tpBinding = mdBinding.getReturnType();
      if (tpBinding != null) {
        usePool.add(Triple.of(node, EdgeType.RETURN_TYPE, tpBinding.getQualifiedName()));
      }
    }

    List<URI> methodParaList = new ArrayList<URI>();

    // para decl and type
    if (!md.parameters().isEmpty()) {
      if (!paramMap.containsKey(declaringClass)) paramMap.put(declaringClass, new HashMap<>());
      if (!paramMap.get(declaringClass).containsKey(mdBinding.getName()))
        paramMap.get(declaringClass).put(mdBinding.getName(), new ArrayList<>());
      // create element nodes
      List<SingleVariableDeclaration> paras = md.parameters();
      for (SingleVariableDeclaration p : paras) {
        String identifier = JdtService.getIdentifier(p);
        if (p.toString().trim().startsWith("@Param")) {
          String[] split = p.toString().trim().split(" ");
          identifier = identifier.replace(".", "/").replaceAll("\\(.+?\\)", "");
          String[] idtfSplit = identifier.split("/");
          idtfSplit[idtfSplit.length - 1] = URI.checkInvalidCh(split[0]);
          identifier = String.join("/", idtfSplit);

          URI uri = new URI(false, uriFilePath);
          uri.addLayer(identifier, Language.JAVA);
          MybatisParam param =
              new MybatisParam(false, split[0], uri, p.getType().toString(), p.toString());
          paramMap.get(declaringClass).get(mdBinding.getName()).add(param);

          methodParaList.add(uri);
        } else {
          identifier = identifier.replace(".", "/").replaceAll("\\(.+?\\)", "");
          URI uri = new URI(false, uriFilePath);
          uri.addLayer(identifier, Language.JAVA);
          MybatisParam param =
              new MybatisParam(false, uri.getSymbol(), uri, p.getType().toString(), p.toString());
          paramMap.get(declaringClass).get(mdBinding.getName()).add(param);

          methodParaList.add(uri);
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

        ITypeBinding paraBinding = p.getType().resolveBinding();
        if (paraBinding != null) {
          usePool.add(Triple.of(pn, EdgeType.DATA_TYPE, paraBinding.getQualifiedName()));
        }
      }
    }

    // annotations
    parseAnnotations(md.modifiers(), node, methodParaList);

    // TODO: add exception types
    if (!md.thrownExceptionTypes().isEmpty()) {}

    // TODO: process body here or else where?
    if (md.getBody() != null) {
      if (!md.getBody().statements().isEmpty()) {
        parseBodyBlock(md.getBody(), qname + ".BLOCK");
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
  private Optional<RelationNode> parseBodyBlock(Block body, String rootName) {
    if (body == null || body.statements().isEmpty()) {
      return Optional.empty();
    }

    // the node of the current block node
    Optional<RelationNode> rootOpt = parseBodyBlock(body);
    if (rootOpt.isPresent()) {
      defPool.put(rootName, rootOpt.get());
      return rootOpt;
    } else {
      return Optional.empty();
    }
  }

  private void parseAnnotations(List modifiers, ElementNode annotatedNode, List<URI> methodParas) {
    for (Object modifier : modifiers) {
      if (modifier instanceof Annotation) {
        Annotation annotation = (Annotation) modifier;

        String query = null, idtf = null, filepath = null;
        Language lang = null;
        if (annotationList.contains(((SimpleName) annotation.getTypeName()).getIdentifier())
            && annotation instanceof SingleMemberAnnotation) {
          query = ((SingleMemberAnnotation) annotation).getValue().toString();
          query = query.substring(1, query.length() - 1);
          if (annotatedNode.getUri() == null) {
            query = null;
          } else {
            idtf =
                annotatedNode.getUri().getIdentifier()
                    + "/"
                    + ((SimpleName) annotation.getTypeName()).getIdentifier();
            lang = annotatedNode.getLanguage();
            filepath = annotatedNode.getUri().getFile();
          }
        }

        List<URI> sqlParams = new ArrayList<>();
        if (query != null) {
          query = StringEscapeUtils.unescapeJava(query);
          Graph<Node, Edge> graph =
              generator.generate(query, FileUtil.getRootPath() + "/" + filepath, lang, idtf, "");
          List<ElementNode> list = generator.getIdentifiers().get("");
          for (ElementNode n : list) {
            if (n.getType() == edu.pku.code2graph.gen.sql.model.NodeType.Parameter) {
              sqlParams.add(n.getUri());
            }
          }
          generator.clearIdentifiers();
        }
        // create node as a child of annotatedNode
        RelationNode node =
            new RelationNode(
                GraphUtil.nid(),
                Language.JAVA,
                NodeType.ANNOTATION,
                annotation.toString(),
                annotation.getTypeName().getFullyQualifiedName());
        node.setRange(computeRange(annotation));

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

        // TODO: #{a.b} <-> Class.field
        if (!sqlParams.isEmpty() && methodParas != null && !methodParas.isEmpty()) {
          for (URI sql_uri : sqlParams) {
            for (URI java_uri : methodParas) {
              String sql_sym = sql_uri.getSymbol();
              sql_sym = sql_sym.substring(2, sql_sym.length() - 1);
              String java_sym = java_uri.getSymbol();
              String[] split = java_sym.split("\"");
              if (split.length == 3) {
                java_sym = java_sym.split("\"")[1];
              }
              if (sql_sym.equals(java_sym)) {
                uriPairs.add(new ImmutablePair<>(sql_uri, java_uri));
              }
            }
          }
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
  private Optional<RelationNode> parseBodyBlock(Block body) {
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
  private void parseArguments(RelationNode node, List arguments) {}

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
          parseArguments(node, ci.arguments());
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
          parseArguments(node, ci.arguments());
          return Optional.of(node);
        }
      case ASTNode.RETURN_STATEMENT:
        {
          Expression expression = ((ReturnStatement) stmt).getExpression();
          if (expression != null) {
            RelationNode node = parseExpression(expression);
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

          forStatement.initializers();
          forStatement.updaters();

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
            RelationNode cond = parseExpression(expression);
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
            RelationNode cond = parseExpression(expression);
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
                  RelationNode condition = parseExpression((Expression) exx);
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
  private RelationNode parseExpression(Expression exp) {
    RelationNode root = new RelationNode(GraphUtil.nid(), Language.JAVA);
    root.setRange(computeRange(exp));

    root.setSnippet(exp.toString());

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
      case ASTNode.QUALIFIED_NAME:
        {
          root.setType(NodeType.QUALIFIED_NAME);
          QualifiedName qualifiedName = (QualifiedName) exp;
          URI uri = new URI(true, uriFilePath);
          uri.addLayer(qualifiedName.getFullyQualifiedName(), Language.JAVA);
          root.setUri(uri);
          break;
        }
      case ASTNode.SIMPLE_NAME:
        {
          IBinding binding = ((SimpleName) exp).resolveBinding();
          URI uri = new URI(true, uriFilePath);
          uri.addLayer(((SimpleName) exp).getFullyQualifiedName(), Language.JAVA);
          root.setUri(uri);
          if (binding == null) {
            // an unresolved identifier
            root.setType(NodeType.SIMPLE_NAME);
            usePool.add(
                Triple.of(root, EdgeType.REFERENCE, ((SimpleName) exp).getFullyQualifiedName()));
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
          root.setType(NodeType.SUPER_FIELD_ACCESS);
          SuperFieldAccess fa = (SuperFieldAccess) exp;

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
          parseArguments(root, cic.arguments());
          break;
        }
      case ASTNode.SUPER_METHOD_INVOCATION:
        {
          SuperMethodInvocation mi = (SuperMethodInvocation) exp;
          root.setType(NodeType.SUPER_METHOD_INVOCATION);

          parseArguments(root, mi.arguments());
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
          root.setType(NodeType.METHOD_INVOCATION);

          parseArguments(root, mi.arguments());

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

          break;
        }
      case ASTNode.INFIX_EXPRESSION:
        {
          InfixExpression iex = (InfixExpression) exp;
          root.setType(NodeType.INFIX);
          root.setSymbol(iex.getOperator().toString());
          root.setArity(2);

          break;
        }
      case ASTNode.PREFIX_EXPRESSION:
        {
          PrefixExpression pex = (PrefixExpression) exp;
          root.setType(NodeType.PREFIX);
          root.setSymbol(pex.getOperator().toString());
          root.setArity(1);

          break;
        }
      case ASTNode.POSTFIX_EXPRESSION:
        {
          PostfixExpression pex = (PostfixExpression) exp;
          root.setType(NodeType.POSTFIX);
          root.setSymbol(pex.getOperator().toString());
          root.setArity(1);
        }
    }

    return root;
  }
}
