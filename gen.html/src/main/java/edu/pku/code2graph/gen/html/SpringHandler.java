package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.gen.html.model.DialectNode;
import edu.pku.code2graph.gen.html.model.NodeType;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

import static edu.pku.code2graph.model.TypeSet.type;

public class SpringHandler extends AbstractHandler {
  private StandardDialectParser dialectParser = new StandardDialectParser();

  public void generateFromDoc(Document doc) {
    stack.clear();
    traverseChidren(doc);
  }

  private void traverseChidren(Element ele) {
    URI uri = new URI(Protocol.DEF, "HTML", filePath, getIdentifier(ele.tagName()));
    ElementNode en =
        new ElementNode(
            GraphUtil.nid(),
            Language.HTML,
            ele instanceof Document ? type("file", true) : type("element", true),
            ele.toString(),
            ele.tagName(),
            ele.normalName(),
            uri);
    logger.debug(
        (ele instanceof Document) + "," + ele + "," + ele.tagName() + "," + ele.normalName());
    logger.debug(uri.getIdentifier());
    graph.addVertex(en);

    if (!stack.isEmpty()) {
      graph.addEdge(stack.peek(), en, new Edge(GraphUtil.eid(), NodeType.CHILD));
    }

    List<Attribute> attrs = ele.attributes().asList();
    attrs.forEach(
        (attr) -> {
          RelationNode rn =
              new RelationNode(
                  GraphUtil.nid(),
                  Language.HTML,
                  NodeType.ATTR,
                  attr.getKey() + "=" + attr.getValue(),
                  attr.getKey());
          logger.debug("attr:" + attr.getKey() + "=" + attr.getValue());
          graph.addVertex(rn);
          graph.addEdge(en, rn, new Edge(GraphUtil.eid(), NodeType.ATTR));

          DialectNode dn = dialectParser.parseTree(attr.getValue());
          if (dn != null) {
            ElementNode attrEn = (ElementNode) DialectNodeToGnode(dn, attr.getKey(), "");
            graph.addEdge(rn, attrEn, new Edge(GraphUtil.eid(), NodeType.INLINE));
          }
        });

    stack.push(en);
    Elements children = ele.children();
    children.forEach(this::traverseChidren);
    stack.pop();
  }

  public Node DialectNodeToGnode(DialectNode node, String attrName, String parentIdtf) {
    DialectNode current = node;
    URI uri = new URI(Protocol.USE, "HTML", filePath, getIdentifier(attrName));
    String curIdtf =
        parentIdtf + ((parentIdtf.isEmpty()) ? "" : "/") + URI.checkInvalidCh(current.getName());
    URI inline = new URI(Protocol.USE, "DIALECT", filePath, curIdtf);
    uri.setInline(inline);
    ElementNode en =
        new ElementNode(
            GraphUtil.nid(),
            Language.DIALECT,
            NodeType.INLINE_VAR,
            current.getSnippet(),
            current.getName(),
            current.getName(),
            uri);
    graph.addVertex(en);

    for (DialectNode child : node.getChildren()) {
      ElementNode childNode = (ElementNode) DialectNodeToGnode(child, attrName, curIdtf);
      graph.addVertex(childNode);
      graph.addEdge(en, childNode, new Edge(GraphUtil.eid(), NodeType.CHILD));
    }

    return en;
  }
}
