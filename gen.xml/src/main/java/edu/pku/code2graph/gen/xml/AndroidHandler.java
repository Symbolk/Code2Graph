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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static edu.pku.code2graph.model.TypeSet.type;

/**
 * Dedicated handler for view xml in Android code Layout xml files are used to define the actual
 * UI(User interface) of our application. It holds all the elements(views) or the tools that we want
 * to use in our application. Like the TextView’s, Button’s and other UI elements.
 */
public class AndroidHandler extends AbstractHandler {
  private Locator locator;
  private Stack<Locator> locatorStack = new Stack<>();

  private Map<Node, String> pathMap = new HashMap<>();

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
    logger.debug("\nEnd Parsing {}", filePath);
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
    String qName = name;
    String parentDir = FileUtil.getParentFolderName(filePath);
    // e.g. @layout/xxx, @menu/xxx, could be referenced by other xml and java
    if (!parentDir.contains("-") && !parentDir.startsWith("values")) {
      qName = "@" + parentDir + "/" + FilenameUtils.removeExtension(name);
    }

    URI uri = new URI(Protocol.DEF, Language.XML, filePath, null);

    ElementNode root =
        new ElementNode(GraphUtil.nid(), Language.XML, type("file", true), "", name, qName, uri);
    graph.addVertex(root);
    stack.push(root);
    uriMap.put(root.getUri(), root);
    logger.debug("Start Parsing {}", filePath);
    super.startDocument();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    Type nType = type(qName, true);

    String idtf = "";
    String parentIdtf = pathMap.get(stack.peek());
    if (stack.size() > 0 && parentIdtf != null) {
      idtf = parentIdtf;
    }
    idtf = idtf + (idtf.isEmpty() ? "" : "/") + URI.checkInvalidCh(qName);
    URI xllUri = new URI(Protocol.DEF, Language.XML, filePath, idtf);

    // qname = tag/type name, name = identifier
    ElementNode en = new ElementNode(GraphUtil.nid(), Language.XML, nType, "", "", "", xllUri);
    pathMap.put(en, idtf);
    // TODO correctly set the start line with locator stack
    en.setRange(
        new Range(
            locator.getLineNumber(),
            locator.getLineNumber(),
            locator.getColumnNumber(),
            locator.getColumnNumber()));
    graph.addVertex(en);
    uriMap.put(en.getUri(), en);
    if (stack.size() > 0) {
      // View is the child of ViewGroup
      graph.addEdge(stack.peek(), en, new Edge(GraphUtil.eid(), CHILD));
    }
    stack.push(en);

    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        String key = attributes.getQName(i);
        String value = attributes.getValue(i);
        String idtfLayer = en.getUri().getIdentifier();

        // each attribute should only be processed once
        // either for def, or for ref
        // definitions (id)
        if ("name".equals(key)) {
          // for resources
          // ref in java: R.qname.value
          // ref in xml: @qname/value
          String resName = "@" + qName + "/" + value;
          en.getUri().setIdentifier(idtfLayer + "/name");
          en.setName(value);
          en.setQualifiedName(resName);

          URI inline = new URI();
          inline.setIdentifier(URI.checkInvalidCh(value));
          en.getUri().setProtocol(Protocol.DEF);
          en.getUri().setInline(inline);

          defPool.put(resName, en);
        } else if ("android:id".equals(key)) {
          // fr components
          if (value.startsWith("@+")) {
            en.setName(value);
            en.getUri().setIdentifier(idtfLayer + "/" + URI.checkInvalidCh("android:id"));
            String identifier = value.replace("+", "");
            en.setQualifiedName(identifier);

            URI inline = new URI();
            inline.setIdentifier(URI.checkInvalidCh(value));
            en.getUri().setProtocol(Protocol.DEF);
            en.getUri().setInline(inline);

            defPool.put(identifier, en);
          }
        } else {
          // references
          if (value.startsWith("@") && !value.startsWith("@android:")) {
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
    }

    super.startElement(uri, localName, qName, attributes);
    // Keep snapshot of start location, for later when end of element is found.
    locatorStack.push(new LocatorImpl(locator));
  }
}
