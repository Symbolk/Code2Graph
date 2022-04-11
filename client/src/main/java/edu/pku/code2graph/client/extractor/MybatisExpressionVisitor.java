package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.gen.jdt.ExpressionVisitor;
import edu.pku.code2graph.gen.jdt.JdtService;
import edu.pku.code2graph.gen.jdt.model.EdgeType;
import edu.pku.code2graph.gen.jdt.model.NodeType;
import edu.pku.code2graph.gen.sql.JsqlGenerator;
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
public class MybatisExpressionVisitor extends ExpressionVisitor {

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
              JdtService.getIdentifier(fragment),
              (layer) -> {
                layer.put("varType", fd.getType().toString());
              });

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

    String declaringClass = null;
    if (mdBinding == null) {
      return true;
    }
    if (mdBinding.getDeclaringClass() != null) {
      declaringClass = mdBinding.getDeclaringClass().getQualifiedName();
    }

    // can be null for method in local anonymous class
    String name = md.getName().getFullyQualifiedName();
    String qname = name;
    if (mdBinding != null) {
      qname = JdtService.getMethodQNameFromBinding(mdBinding);
    }

    List<URI> methodParaList = new ArrayList<URI>();

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

    // para decl and type
    if (!md.parameters().isEmpty()) {
      if (!paramMap.containsKey(declaringClass)) paramMap.put(declaringClass, new HashMap<>());
      if (!paramMap.get(declaringClass).containsKey(mdBinding.getName()))
        paramMap.get(declaringClass).put(mdBinding.getName(), new ArrayList<>());
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
          identifier = identifier + '/' + URI.checkInvalidCh(split[0].split("\\(")[0]);

          String para_qname = split[0];
          String para_name =
              para_qname.substring(para_qname.indexOf('"') + 1, para_qname.length() - 2);

          URI uri = new URI(false, uriFilePath);
          uri.addLayer(identifier, Language.JAVA);
          uri.addLayer(para_name, Language.ANY);

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

          MybatisParam param =
              new MybatisParam(false, split[0], uri, p.getType().toString(), p.toString());
          paramMap.get(declaringClass).get(mdBinding.getName()).add(param);
          methodParaList.add(uri);
        } else {
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

          node.setRange(computeRange(p));
          graph.addEdge(node, pn, new Edge(GraphUtil.eid(), EdgeType.PARAMETER));

          URI uri = pn.getUri();
          MybatisParam param =
              new MybatisParam(false, uri.getSymbol(), uri, p.getType().toString(), p.toString());
          paramMap.get(declaringClass).get(mdBinding.getName()).add(param);
          methodParaList.add(uri);
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
        parseBodyBlock(md.getBody(), md.getName().toString(), qname)
            .ifPresent(
                blockNode ->
                    graph.addEdge(node, blockNode, new Edge(GraphUtil.eid(), EdgeType.BODY)));
      }
    }
    return true;
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
              generator.generate(
                  query,
                  FileUtil.getRootPath() + "/" + filepath,
                  lang,
                  idtf,
                  annotatedNode.getUri(),
                  "");
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
        node.setUri(createIdentifier(identifier));

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
}
