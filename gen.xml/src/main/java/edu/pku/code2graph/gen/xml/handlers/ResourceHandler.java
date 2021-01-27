package edu.pku.code2graph.gen.xml.handlers;

import edu.pku.code2graph.gen.xml.AbstractHandler;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Type;
import edu.pku.code2graph.util.GraphUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Stack;

import static edu.pku.code2graph.model.TypeSet.type;

/**
 * Resources are used for anything from defining colors, images, layouts, menus, and string values.
 * The value of this is that nothing is hardcoded. Everything is defined in these resource files and
 * then can be referenced within your application's code. The simplest of these resources and the
 * most common is using string resources to allow for flexible, localized text.
 */
public class ResourceHandler extends AbstractHandler {
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

    // qname = tag/type name, name = identifier
    ElementNode en = new ElementNode(GraphUtil.nid(), nType, "", "", "");
    graph.addVertex(en);
    if (stack.size() > 0) {
      graph.addEdge(stack.peek(), en, new Edge(GraphUtil.eid(), CHILD));
    }
    stack.push(en);

    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        String key = attributes.getQName(i);
        String value = attributes.getValue(i);
        if ("name".equals(key)) {
          // ref in java: R.qname.value
          // ref in xml: @qname/value
          String resName = "@" + qName + "/" + value;
          en.setName(value);
          en.setQualifiedName(resName);
          defPool.put(resName, en);
        }
      }
    }

    super.startElement(uri, localName, qName, attributes);
  }
}
