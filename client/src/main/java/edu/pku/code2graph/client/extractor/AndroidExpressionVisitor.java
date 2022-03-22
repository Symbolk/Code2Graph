package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.gen.jdt.ExpressionVisitor;
import edu.pku.code2graph.gen.jdt.JdtService;
import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class AndroidExpressionVisitor extends ExpressionVisitor {
  private ElementNode cuNode;
  private Map<String, List<Pair<String, URI>>> layouts;
  private Map<String, List<URI>> ids;
  private Map<String, List<URI>> dataBindings;
  private Map<String, List<Pair<String, URI>>> dataBindingClassInFile;

  public AndroidExpressionVisitor(
      Map<String, List<Pair<String, URI>>> layMap,
      Map<String, List<URI>> idMap,
      Map<String, List<Pair<String, URI>>> dataBindingClassInFile,
      Map<String, List<URI>> dataBindings) {
    layouts = layMap;
    ids = idMap;
    this.dataBindingClassInFile = dataBindingClassInFile;
    this.dataBindings = dataBindings;
  }

  private void addDataBinding(URI bindingName) {
    if (!dataBindings.containsKey(filePath)) {
      dataBindings.put(filePath, new ArrayList<>());
    }
    dataBindings.get(filePath).add(bindingName);
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
          QualifiedName qualifiedName = (QualifiedName) exp;
          root.setType(NodeType.QUALIFIED_NAME);
          URI uri = createIdentifier(qualifiedName.getFullyQualifiedName());
          root.setUri(uri);
          GraphUtil.addNode(root);

          if (exp.toString().startsWith("R.layout.") || exp.toString().startsWith("R.menu.")) {
            if (!layouts.containsKey(filePath)) {
              layouts.put(filePath, new ArrayList<>());
            }
            layouts.get(filePath).add(new MutablePair<>(exp.toString(), uri));
          } else if (exp.toString().startsWith("R.id.")) {
            if (!ids.containsKey(filePath)) {
              ids.put(filePath, new ArrayList<>());
            }
            ids.get(filePath).add(uri);
          } else {
            String[] expSplit = exp.toString().split("\\.");
            if (expSplit.length == 2 && expSplit[0].toLowerCase().contains("binding")) {
              addDataBinding(uri);
            }
          }
          break;
        }
      case ASTNode.SIMPLE_NAME:
        {
          SimpleName name = (SimpleName) exp;
          String identifier = name.getFullyQualifiedName();
          IBinding binding = name.resolveBinding();
          URI uri = createIdentifier(identifier);
          root.setUri(uri);
          GraphUtil.addNode(root);

          if (exp.toString().startsWith("R.layout.") || exp.toString().startsWith("R.menu.")) {
            if (!layouts.containsKey(filePath)) {
              layouts.put(filePath, new ArrayList<>());
            }
            layouts.get(filePath).add(new MutablePair<>(exp.toString(), uri));
          } else if (exp.toString().startsWith("R.id.")) {
            if (!ids.containsKey(filePath)) {
              ids.put(filePath, new ArrayList<>());
            }
            ids.get(filePath).add(uri);
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
          FieldAccess fa = (FieldAccess) exp;
          break;
        }
      case ASTNode.CLASS_INSTANCE_CREATION:
        {
          root.setType(NodeType.TYPE_INSTANTIATION);

          ClassInstanceCreation cic = (ClassInstanceCreation) exp;
          String identifier = cic.getType().toString();
          pushScope(identifier);
          parseArguments(cic.arguments());
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
          parseArguments(mi.arguments());
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
          parseArguments(mi.arguments());
          popScope();
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

  private String capToSlash(String cap) {
    Pattern capPattern = Pattern.compile("[A-Z]");
    String camel = cap.substring(1);
    Matcher matcher = capPattern.matcher(camel);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
    }
    matcher.appendTail(sb);
    return cap.substring(0, 1).toLowerCase() + sb.toString();
  }

  private void addBindingClass(URI uri, String baseType) {
    if (!baseType.endsWith("Binding")) {
      return;
    }

    if (!dataBindingClassInFile.containsKey(filePath)) {
      dataBindingClassInFile.put(filePath, new ArrayList<>());
    }
    dataBindingClassInFile
        .get(filePath)
        .add(new MutablePair<>(capToSlash(baseType.substring(0, baseType.length() - 7)), uri));
  }

  @Override
  public boolean visit(FieldDeclaration fd) {
    List<VariableDeclarationFragment> fragments = fd.fragments();
    String baseType = fd.getType().toString();
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

      if (baseType.endsWith("Binding")) {
        addBindingClass(node.getUri(), baseType);
      }

      // annotations
      parseAnnotations(fd.modifiers(), node);

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

        String baseType = p.getType().toString();
        if (baseType.endsWith("Binding")) {
          addBindingClass(pn.getUri(), baseType);
        }

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
    return true;
  }
}
