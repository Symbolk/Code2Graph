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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Visitor focusing on the entity granularity, at or above member level (cu/type/member) */
public class MemberVisitor extends AbstractJdtVisitor {
  private ElementNode root;

  @Override
  public boolean visit(CompilationUnit cu) {
    ElementNode cuNode =
        new ElementNode(
            GraphUtil.nid(),
            Language.JAVA,
            NodeType.FILE,
            "",
            FileUtil.getFileNameFromPath(filePath),
            filePath);
    graph.addVertex(cuNode);
    this.root = cuNode;

    logger.info("Start Parsing {}", filePath);
    return true;
  }

  @Override
  public boolean visit(TypeDeclaration td) {
    Type type = td.isInterface() ? NodeType.INTERFACE_DECLARATION : NodeType.CLASS_DECLARATION;
    ITypeBinding tdBinding = td.resolveBinding();
    // isFromSource
    String qname = tdBinding.getQualifiedName();

    ElementNode node =
        new ElementNode(
            GraphUtil.nid(), Language.JAVA, type, td.toString(), td.getName().toString(), qname);
    node.setRange(computeRange(td));
    graph.addVertex(node);
    defPool.put(qname, node);

    if (td.isPackageMemberTypeDeclaration()) {
      graph.addEdge(root, node, new Edge(GraphUtil.eid(), EdgeType.CHILD));
    }

    parseMembers(td, tdBinding, node);

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
          ElementNode initNode =
              new ElementNode(
                  GraphUtil.nid(),
                  Language.JAVA,
                  NodeType.INIT_BLOCK_DECLARATION,
                  initializer.toString(),
                  node.getName() + ".INIT",
                  qname);
          initNode.setRange(computeRange(initializer));
          defPool.put(qname, node);

          graph.addVertex(initNode);
          graph.addEdge(node, initNode, new Edge(GraphUtil.eid(), EdgeType.CHILD));
        }
      }
    }

    IVariableBinding[] fdBindings = binding.getDeclaredFields();
    for (IVariableBinding b : fdBindings) {
      usePool.add(Triple.of(node, EdgeType.CHILD, parentQName + "." + b.getName()));
    }

    IMethodBinding[] mdBindings = binding.getDeclaredMethods();
    for (IMethodBinding b : mdBindings) {
      if (b.isDefaultConstructor()) {
        continue;
      }
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
      ElementNode node =
          new ElementNode(
              GraphUtil.nid(),
              Language.JAVA,
              NodeType.FIELD_DECLARATION,
              fragment.toString(),
              name,
              qname);
      node.setRange(computeRange(fragment));

      graph.addVertex(node);
      defPool.put(qname, node);

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
    ElementNode node =
        new ElementNode(
            GraphUtil.nid(),
            Language.JAVA,
            NodeType.METHOD_DECLARATION,
            md.toString(),
            name,
            qname);
    node.setRange(computeRange(md));

    graph.addVertex(node);
    defPool.put(qname, node);

    // return type
    if (mdBinding != null) {
      ITypeBinding tpBinding = mdBinding.getReturnType();
      if (tpBinding != null && tpBinding.isFromSource()) {
        usePool.add(Triple.of(node, EdgeType.METHOD_RETURN, tpBinding.getQualifiedName()));
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
            new ElementNode(
                GraphUtil.nid(),
                Language.JAVA,
                NodeType.VAR_DECLARATION,
                p.toString(),
                para_name,
                para_qname);
        node.setRange(computeRange(p));

        graph.addVertex(pn);
        graph.addEdge(node, pn, new Edge(GraphUtil.eid(), EdgeType.METHOD_PARAMETER));
        defPool.put(para_qname, pn);

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

  @Override
  public boolean visit(QualifiedName qn) {
    // qn: R.a.b
    if (qn.getQualifier().isQualifiedName()) { // R.a
      if ("R".equals(((QualifiedName) qn.getQualifier()).getQualifier().toString())) { // R
        // find ancestor entity name
        Optional<String> parentEntityName = JdtService.findWrappedEntityName(qn);
        if (parentEntityName.isPresent()) {
          // find in def pool (should already be visited)
          Optional<Node> nodeOpt = findEntityNodeByName(parentEntityName.get());
          // add into usepool
          nodeOpt.ifPresent(
              value ->
                  GraphUtil.addCrossLangRef(
                      Triple.of(value, EdgeType.REFERENCE, qn.getFullyQualifiedName())));
        }
      }
    }
    return false;
  }

  private Optional<Node> findEntityNodeByName(String name) {
    if (defPool.containsKey(name)) {
      return Optional.of(defPool.get(name));
    } else {
      // greedily match as simple name
      return defPool.entrySet().stream()
          .filter(e -> e.getKey().endsWith(name))
          .map(Map.Entry::getValue)
          .findFirst();
    }
  }

  //    @Override
  //    public boolean visit(Initializer initializer) {
  //      initializer.getParent().getParent();
  //      return false;
  //    }

  @Override
  public boolean visit(FieldAccess fa) {
    IVariableBinding faBinding = fa.resolveFieldBinding();
    if (faBinding != null && faBinding.isField() && faBinding.getDeclaringClass() != null) {
      // find wrapped parent entity node
      Optional<String> parentEntityName = JdtService.findWrappedEntityName(fa);
      if (parentEntityName.isPresent()) {
        Optional<Node> nodeOpt = findEntityNodeByName(parentEntityName.get());
        // add to use pool
        nodeOpt.ifPresent(
            node ->
                usePool.add(
                    Triple.of(
                        node,
                        EdgeType.REFERENCE,
                        faBinding.getDeclaringClass().getQualifiedName()
                            + "."
                            + faBinding.getName())));
      }
    }
    return true;
  }

  @Override
  public boolean visit(MethodInvocation mi) {
    IMethodBinding mdBinding = mi.resolveMethodBinding();
    // only internal invocation (or consider types, fields and local?)
    if (mdBinding != null) {
      // get caller qname
      JdtService.findWrappedMethodName(mi)
          .ifPresent(
              name -> {
                usePool.add(Triple.of(root, EdgeType.METHOD_CALLER, name));
              });
      // get callee qname
      usePool.add(
          Triple.of(root, EdgeType.METHOD_CALLEE, JdtService.getMethodQNameFromBinding(mdBinding)));
    }
    return true;
  }
}
