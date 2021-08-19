package edu.pku.code2graph.gen.html;

import edu.pku.code2graph.gen.html.model.NodeType;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

import static edu.pku.code2graph.model.TypeSet.type;

public class DocumentHandler extends AbstractHandler {
  public void generateFromDoc(Document doc) {
    stack.clear();
    traverseChidren(doc);
  }

  private void traverseChidren(Element ele) {
    URI uri = new URI(Protocol.DEF, Language.HTML, filePath, getIdentifier(ele.tagName()));
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
        });

    stack.push(en);
    Elements children = ele.children();
    children.forEach(this::traverseChidren);
    stack.pop();
  }
}
