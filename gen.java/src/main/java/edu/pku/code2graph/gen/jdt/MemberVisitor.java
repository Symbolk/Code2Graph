package edu.pku.code2graph.gen.jdt;

import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.eclipse.jdt.core.dom.*;
import org.jgrapht.alg.util.Triple;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Visitor focusing on the entity granularity, at or above member level (cu/type/member) */
public class MemberVisitor extends AbstractJdtVisitor {
  private ElementNode root;

  @Override
  public boolean visit(CompilationUnit cu) {
    ElementNode cuNode = createElementNode(
            NodeType.FILE,
            "",
            FileUtil.getFileNameFromPath(filePath),
            filePath,
            "");

    // TODO get relative path if given a base path
    //    FileUtil.getRelativePath(
    //            basePath, cuNode.getQualifiedName());
    setPackageAttr(cuNode, cu.getPackage().getName().getFullyQualifiedName());
    graph.addVertex(cuNode);
    this.root = cuNode;

    logger.debug("Start Parsing {}", filePath);
    return true;
  }

  @Override
  public boolean visit(TypeDeclaration td) {
    Type type = td.isInterface() ? NodeType.INTERFACE_DECLARATION : NodeType.CLASS_DECLARATION;
    ITypeBinding tdBinding = td.resolveBinding();
    // isFromSource
    String qname = td.getName().getFullyQualifiedName();
    if (tdBinding != null) {
      qname = tdBinding.getQualifiedName();
    } else {
      qname = JdtService.getTypeQNameFromParents(td);
    }

    ElementNode node = createElementNode(
            type,
            td.toString(),
            td.getName().toString(),
            qname,
            JdtService.getIdentifier(td));

    node.setRange(computeRange(td));
    setModifierAttr(node, td.modifiers());
    setTypeAttr(node, qname);

    System.out.println("edge");
    graph.addEdge(root, node, new Edge(GraphUtil.eid(), EdgeType.CHILD));
    if (tdBinding != null) {
      setPackageAttr(node, tdBinding.getPackage().getName());
    }

    //    if (td.isPackageMemberTypeDeclaration()) {
    //
    //    } else if (td.isLocalTypeDeclaration()) {
    //      // find parent method node
    //
    //    } else if (td.isMemberTypeDeclaration()) {
    //      // find parent type node
    //
    //    }

    // TODO fix member parsing when tdbinding is null (when file not under main folder)
    if (tdBinding != null) {
      parseMembers(td, tdBinding, node);
    }

    return true;
  }

  /**
   * Parse the members of body declarations, just the declaration signature, visit body block in
   * other visitors
   *
   * @param binding
   * @param node
   */
  private void parseMembers(
      AbstractTypeDeclaration declaration, ITypeBinding binding, ElementNode node) {
    List<BodyDeclaration> bodyDeclarations = declaration.bodyDeclarations();
    if (bodyDeclarations.isEmpty()) {
      return;
    }
    String parentQName = node.getQualifiedName();

    List<Initializer> initializers =
        bodyDeclarations.stream()
            .filter(Initializer.class::isInstance)
            .map(Initializer.class::cast)
            .collect(Collectors.toList());
    if (!initializers.isEmpty()) {
      for (Initializer initializer : initializers) {
        if (!initializer.getBody().statements().isEmpty()) {
          String qname = parentQName + ".INIT";

          ElementNode initNode = createElementNode(
                  NodeType.INIT_BLOCK_DECLARATION,
                  initializer.toString(),
                  node.getName() + ".INIT",
                  qname,
                  JdtService.getIdentifier(initializer));
                  
          initNode.setRange(computeRange(initializer));
          setPackageAttr(node, cu.getPackage().getName().getFullyQualifiedName());

          defPool.put(qname, node);

          System.out.println("edge");
          graph.addEdge(node, initNode, new Edge(GraphUtil.eid(), EdgeType.CHILD));
        }
      }
    }

    IVariableBinding[] fdBindings = binding.getDeclaredFields();
    for (IVariableBinding b : fdBindings) {
      System.out.println("edge");
      usePool.add(Triple.of(node, EdgeType.CHILD, parentQName + "." + b.getName()));
    }

    IMethodBinding[] mdBindings = binding.getDeclaredMethods();
    for (IMethodBinding b : mdBindings) {
      if (b.isDefaultConstructor()) {
        continue;
      }
      System.out.println("edge");
      usePool.add(Triple.of(node, EdgeType.CHILD, JdtService.getMethodQNameFromBinding(b)));
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

      ElementNode node = createElementNode(
              NodeType.FIELD_DECLARATION,
              fragment.toString(),
              name,
              qname,
              JdtService.getIdentifier(fragment));

      node.setRange(computeRange(fragment));
      setModifierAttr(node, fd.modifiers());
      root.getAttribute("package").ifPresent(pkg -> setPackageAttr(node, (String) pkg));

      if (binding != null && binding.getType().isFromSource()) {
        usePool.add(Triple.of(node, EdgeType.DATA_TYPE, binding.getType().getQualifiedName()));
      }

      if (fragment.getInitializer() != null) {
        //        parseExpression(fragment.getInitializer())
        //            .ifPresent(
        //                res -> GraphUtil.addCrossLangRef(Triple.of(node, EdgeType.REFERENCE,
        // res)));
      }
    }
    return true;
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

    ElementNode node = createElementNode(
            NodeType.METHOD_DECLARATION,
            md.toString(),
            name,
            qname,
            JdtService.getIdentifier(md));

    node.setRange(computeRange(md));
    setModifierAttr(node, md.modifiers());
    root.getAttribute("package").ifPresent(pkg -> setPackageAttr(node, (String) pkg));

    // return type
    if (mdBinding != null) {
      ITypeBinding tpBinding = mdBinding.getReturnType();
      if (tpBinding != null && tpBinding.isFromSource()) {
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
          para_qname = qname + "." + para_name;
        }

        ElementNode pn = createElementNode(
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

    return true;
  }

  /**
   * Bind field access, constant, arg type, method access entities in enclosing statement, and also
   * the enclosing entity
   *
   * @param qn
   * @return
   */
  @Override
  public boolean visit(QualifiedName qn) {
    // enum constant access
    IBinding binding = qn.resolveBinding();
    if (binding instanceof IVariableBinding) {
      IVariableBinding variableBinding = (IVariableBinding) binding;
      if (variableBinding.getDeclaringClass() != null) {
        if (variableBinding.isField() || variableBinding.isEnumConstant()) {
          JdtService.findWrappedEntityName(qn)
              .flatMap(this::findEntityNodeByName)
              .ifPresent(
                  node ->
                      usePool.add(
                          Triple.of(
                              node,
                              EdgeType.REFERENCE,
                              variableBinding.getDeclaringClass().getQualifiedName()
                                  + "."
                                  + variableBinding.getName())));
        }
      }
    }

    return false;
  }

  @Override
  public boolean visit(FieldAccess fa) {
    IVariableBinding faBinding = fa.resolveFieldBinding();
    if (faBinding != null && faBinding.isField() && faBinding.getDeclaringClass() != null) {
      // find wrapped parent entity node and build usage
      JdtService.findWrappedEntityName(fa)
          .ifPresent(entityName -> createFieldUsage(faBinding, entityName));
    }
    return true;
  }

  @Override
  public boolean visit(SimpleName sn) {
    // TODO local enum constant access?
    // find field access from simple names
    if (!sn.isDeclaration()) {
      IBinding binding = sn.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          JdtService.findWrappedEntityName(sn)
              .ifPresent(entityName -> createFieldUsage(variableBinding, entityName));
        }
      }
    }

    return false;
  }

  private void createFieldUsage(IVariableBinding variableBinding, String wrappedEntityName) {
    if (variableBinding.getDeclaringClass() != null) {
      Optional<Node> nodeOpt = findEntityNodeByName(wrappedEntityName);
      // add to use pool
      nodeOpt.ifPresent(
          node ->
              usePool.add(
                  Triple.of(
                      node,
                      EdgeType.REFERENCE,
                      variableBinding.getDeclaringClass().getQualifiedName()
                          + "."
                          + variableBinding.getName())));
    }
  }

  //    @Override
  //    public boolean visit(Initializer initializer) {
  //      initializer.getParent().getParent();
  //      return false;
  //    }

  @Override
  public boolean visit(MethodInvocation mi) {
    IMethodBinding mdBinding = mi.resolveMethodBinding();
    // only internal invocation (or consider types, fields and local?)
    if (mdBinding != null) {
      String calleeSign = JdtService.getMethodQNameFromBinding(mdBinding);
      // get caller qname and find caller node (should have been in defpool)
      JdtService.findWrappedMethodName(mi)
          .ifPresent(
              name -> {
                Node callerNode = defPool.get(name);
                if (callerNode != null) {
                  usePool.add(Triple.of(callerNode, EdgeType.CALLER, calleeSign));
                }
              });
      //      // get callee qname
      //      usePool.add(
      //          Triple.of(root, EdgeType.METHOD_CALLEE, calleeSign));
    }
    return true;
  }

  private void setTypeAttr(Node node, String typeName) {
    if (!typeName.isEmpty()) {
      node.setAttribute("type", typeName);
    }
  }

  private void setPackageAttr(Node node, String packageName) {
    if (!packageName.isEmpty()) {
      node.setAttribute("package", packageName);
    }
  }

  private void setModifierAttr(Node node, List modifiers) {
    for (var obj : modifiers) {
      if (obj instanceof Modifier) {
        Modifier modifier = (Modifier) obj;
        if (modifier.isPublic()) {
          node.setAttribute("access", "public");
        } else if (modifier.isProtected()) {
          node.setAttribute("access", "protected");
        } else if (modifier.isDefault()) {
          node.setAttribute("access", "default");
        } else if (modifier.isPrivate()) {
          node.setAttribute("access", "private");
        }
      }
    }
  }
}
