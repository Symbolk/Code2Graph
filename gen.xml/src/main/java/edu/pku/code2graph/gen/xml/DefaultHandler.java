package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Triple;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Stack;

import static edu.pku.code2graph.model.TypeSet.type;

/**
 * Layout xml files are used to define the actual UI(User interface) of our application. It holds
 * all the elements(views) or the tools that we want to use in our application. Like the TextView’s,
 * Button’s and other UI elements.
 */
public class DefaultHandler extends AbstractHandler {
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
      // View is the child of ViewGroup
      graph.addEdge(stack.peek(), en, new Edge(GraphUtil.eid(), CHILD));
    }
    stack.push(en);

    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        String key = attributes.getQName(i);
        String value = attributes.getValue(i);

        // each attribute should only be processed once
        // either for def, or for ref
        // definitions (id)
        if ("name".equals(key)) {
          // for resources
          // ref in java: R.qname.value
          // ref in xml: @qname/value
          String resName = "@" + qName + "/" + value;
          en.setName(value);
          en.setQualifiedName(resName);
          defPool.put(resName, en);
        } else if ("android:id".equals(key)) {
          // fr components
          if (value.startsWith("@+")) {
            en.setName(value);
            String identifier = value.replace("+", "");
            en.setQualifiedName(identifier);
            defPool.put(identifier, en);
          }
        } else {
          // references
          if (value.startsWith("@") && !value.startsWith("@android:")) {
            Type eType = type(key);
            RelationNode rn = new RelationNode(GraphUtil.nid(), eType, key + "=" + value);
            graph.addVertex(rn);
            graph.addEdge(en, rn, new Edge(GraphUtil.eid(), eType));

            // unified references (may should use regex for matching)
            usePool.add(Triple.of(rn, eType, value));
          }
        }
      }
    }

    super.startElement(uri, localName, qName, attributes);
  }
}
