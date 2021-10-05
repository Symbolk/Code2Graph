package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.alg.util.Triple;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.LocatorImpl;

import java.util.Stack;

import static edu.pku.code2graph.model.TypeSet.type;

/** (Expected) General handler for common xml code */
public class GeneralHandler extends AbstractHandler {
  private Locator locator;
  private Stack<Locator> locatorStack = new Stack<>();

  @Override
  public void setDocumentLocator(Locator locator) {
    super.setDocumentLocator(locator);
    this.locator = locator;
  }

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
    super.endDocument();
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (stack.size() > 1) {
      stack.pop();
    }
    super.endElement(uri, localName, qName);
    if (locatorStack.size() > 0) {
      Locator startLocator = locatorStack.pop();
      startLocator.getLineNumber();
    }
  }

  @Override
  public void startDocument() throws SAXException {
    // set qualified name in the intermediate form: R.type.name
    String name = FileUtil.getFileNameFromPath(filePath);
    String parentDir = FileUtil.getParentFolderName(filePath);
    String qName = parentDir + "/" + FilenameUtils.removeExtension(name);

    ElementNode root =
        new ElementNode(GraphUtil.nid(), Language.XML, type("file", true), "", name, qName);
    graph.addVertex(root);
    stack.push(root);
    logger.debug("Start Parsing {}", uriFilePath);
    super.startDocument();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    Type nType = type(qName, true);

    // qname = tag/type name, name = id
    ElementNode en = new ElementNode(GraphUtil.nid(), Language.XML, nType, "", "", "");
    // TODO correctly set the start line with locator stack
    en.setRange(
        new Range(
            locator.getLineNumber(),
            locator.getLineNumber(),
            locator.getColumnNumber(),
            locator.getColumnNumber()));
    graph.addVertex(en);

    if (stack.size() > 0) {
      graph.addEdge(stack.peek(), en, new Edge(GraphUtil.eid(), CHILD));
    }
    stack.push(en);

    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        String key = attributes.getQName(i);
        String value = attributes.getValue(i);

        // each attribute should only be processed once
        // either for def, or for ref (need to have schema to check key/keyref refs)
        if ("id".equals(key)) {
          // definitions (id)
          String resName = qName + "/" + value;
          en.setName(value);
          en.setQualifiedName(resName);
          defPool.put(resName, en);
        } else {
          // references
          Type eType = type(key);
          RelationNode rn =
              new RelationNode(GraphUtil.nid(), Language.XML, eType, key + "=" + value);
          // TODO correctly set the start line with locator stack
          rn.setRange(
              new Range(
                  locator.getLineNumber(),
                  locator.getLineNumber(),
                  locator.getColumnNumber(),
                  locator.getColumnNumber()));
          graph.addVertex(rn);
          graph.addEdge(en, rn, new Edge(GraphUtil.eid(), eType));

          // unified references (may should use regex for matching)
          usePool.add(Triple.of(rn, eType, value));
        }
      }
    }

    super.startElement(uri, localName, qName, attributes);
    // Keep snapshot of start location, for later when end of element is found.
    locatorStack.push(new LocatorImpl(locator));
  }
}
