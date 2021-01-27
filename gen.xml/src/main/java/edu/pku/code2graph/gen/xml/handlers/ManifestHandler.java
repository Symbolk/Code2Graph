package edu.pku.code2graph.gen.xml.handlers;

import edu.pku.code2graph.gen.xml.AbstractHandler;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.RelationNode;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.alg.util.Triple;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Stack;

import static edu.pku.code2graph.model.TypeSet.type;

/**
 * Manifest xml File(AndroidManifest.xml) is used to define all the components of our application.
 * It includes the names of our application packages, our Activities, receivers, services and the
 * permissions that our application needs. For Example – Suppose we need to use internet in our app
 * then we need to define Internet permission in this file.
 */
public class ManifestHandler extends AbstractHandler {
  // text content between tag start and end
  private String tempString;
  private Stack<ElementNode> stack = new Stack<>();
  public static final Type CHILD = type("child");

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    tempString = new String(ch, start, length);
    //    System.out.print(tempString);
    super.characters(ch, start, length);
  }

  @Override
  public void endDocument() throws SAXException {
    logger.info("\nEnd Parsing");
    super.endDocument();
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (stack.size() > 1) {
      stack.pop();
    }
    super.endElement(uri, localName, qName);
  }

  @Override
  public void startDocument() throws SAXException {
    logger.info("Start Parsing");
    super.startDocument();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    Type nType = type(qName);

    ElementNode en = new ElementNode(GraphUtil.nid(), nType, "", "", "");
    graph.addVertex(en);
    if (stack.size() > 0) {
      // View is the child of ViewGroup
      graph.addEdge(stack.peek(), en, new Edge(GraphUtil.eid(), CHILD));
    }
    stack.push(en);

    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        String key = attributes.getQName(i);
        String value = attributes.getValue(i);
        // each element should have at most 1 @+id
        if ("android:id".equals(key)) {
          if (value.startsWith("@+")) {
            en.setName(value);
            en.setQualifiedName(value);
            defPool.put(value, en);
          }
        }

        // references
        if (value.startsWith("@") && !value.startsWith("@android:")) {
          Type eType = type(key);
          RelationNode rn = new RelationNode(GraphUtil.nid(), eType, key + "=" + value);
          graph.addVertex(rn);
          graph.addEdge(en, rn, new Edge(GraphUtil.eid(), eType));

          // TODO use a unified identifier as qname for nodes
          if (value.startsWith("@id")) {
            usePool.add(Triple.of(rn, eType, value.replace("@id", "@+id")));
          } else if (value.startsWith("@string") || value.startsWith("@color")) {
            usePool.add(Triple.of(rn, eType, value));
          }
        }
      }
    }

    super.startElement(uri, localName, qName, attributes);
  }
}
