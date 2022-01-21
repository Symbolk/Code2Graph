package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.gen.jdt.AbstractJdtVisitor;
import edu.pku.code2graph.gen.jdt.ExpressionVisitor;
import edu.pku.code2graph.gen.jdt.JdtService;
import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.*;
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
public class AndroidExpressionVisitor extends ExpressionVisitor {
  private ElementNode cuNode;
  private Map<String, List<Pair<String, URI>>> layouts;
  private Map<String, List<URI>> ids;
  private Map<String, Map<String, String>> dataBindings = new HashMap<>();
  private Map<URI, String> dataBindingToLayout;

  public AndroidExpressionVisitor(
      Map<String, List<Pair<String, URI>>> layMap,
      Map<String, List<URI>> idMap,
      Map<URI, String> dataBindingMap) {
    layouts = layMap;
    ids = idMap;
    dataBindingToLayout = dataBindingMap;
  }

  private void addDataBinding(String bindingName, String layoutName) {
    if (!dataBindings.containsKey(filePath)) {
      dataBindings.put(filePath, new HashMap<>());
    }
    dataBindings.get(filePath).put(bindingName, layoutName);
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
            if (dataBindings.containsKey(filePath)
                && dataBindings
                    .get(filePath)
                    .containsKey(qualifiedName.getQualifier().toString())) {
              String layoutName =
                  dataBindings.get(filePath).get(qualifiedName.getQualifier().toString());
              dataBindingToLayout.put(uri, layoutName);
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
