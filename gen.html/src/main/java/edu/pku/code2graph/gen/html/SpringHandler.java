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
    URI uri =
        new URI(
            Protocol.DEF,
            Language.HTML,
            uriFilePath,
            ele instanceof Document ? "" : getIdentifier(ele.tagName()));
    ElementNode en =
        new ElementNode(
            GraphUtil.nid(),
            Language.HTML,
            ele instanceof Document ? type("file", true) : type("element", true),
            ele.toString(),
            ele instanceof Document ? "" : ele.tagName(),
            ele instanceof Document ? "" : ele.normalName(),
            uri);
    graph.addVertex(en);
    stack.push(en);

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

          List<DialectNode> dnodes = dialectParser.parseTreeToList(attr.getValue());
          if (!dnodes.isEmpty()) {
            for (DialectNode dn : dnodes) {
              ElementNode attrEn = (ElementNode) DialectNodeToGnode(dn, attr.getKey(), "");
              graph.addEdge(rn, attrEn, new Edge(GraphUtil.eid(), NodeType.INLINE));
            }
          }
        });

    Elements children = ele.children();
    children.forEach(this::traverseChidren);
    stack.pop();
  }

  public Node DialectNodeToGnode(DialectNode node, String attrName, String parentIdtf) {
    DialectNode current = node;
    URI uri = new URI(Protocol.USE, Language.HTML, uriFilePath, getIdentifier(attrName));
    String curIdtf =
        parentIdtf + ((parentIdtf.isEmpty()) ? "" : "/") + URI.checkInvalidCh(current.getName());
    URI inline = new URI(Protocol.USE, Language.OTHER, uriFilePath, curIdtf);
    uri.setInline(inline);
    ElementNode en =
        new ElementNode(
            GraphUtil.nid(),
            Language.OTHER,
            NodeType.INLINE_VAR,
            current.getSnippet(),
            current.getName(),
            current.getName(),
            uri);
    graph.addVertex(en);
    GraphUtil.addURI(Language.HTML, en.getUri(), en);

    for (DialectNode child : node.getChildren()) {
      ElementNode childNode = (ElementNode) DialectNodeToGnode(child, attrName, curIdtf);
      graph.addEdge(en, childNode, new Edge(GraphUtil.eid(), NodeType.CHILD));
    }

    return en;
  }
}
