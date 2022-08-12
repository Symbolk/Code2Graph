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

import java.io.*;
import java.util.*;

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

  private List<String> sourceFile = new ArrayList<>();
  private Range startElePosition = new Range(1, -1, 1, -1);

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    tempString = new String(ch, start, length);
    //    System.out.print(tempString);

    updateStartElePostion(locator);
    super.characters(ch, start, length);
  }

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    updateStartElePostion(locator);
    super.ignorableWhitespace(ch, start, length);
  }

  @Override
  public void endDocument() throws SAXException {
    sourceFile.clear();
    startElePosition.setStartPosition(1, -1);
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

    updateStartElePostion(locator);
  }

  @Override
  public void startDocument() throws SAXException {
    // read in source file with buffer
    try {
      FileInputStream iptStream = new FileInputStream(filePath);
      BufferedReader reader = new BufferedReader(new InputStreamReader(iptStream));
      String line = null;
      while ((line = reader.readLine()) != null) {
        sourceFile.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    // set qualified name in the intermediate form: R.type.name
    String name = FileUtil.getFileNameFromPath(filePath);
    String qName = name;
    String parentDir = FileUtil.getParentFolderName(filePath);
    // e.g. @layout/xxx, @menu/xxx, could be referenced by other xml and java
    if (!parentDir.contains("-") && !parentDir.startsWith("values")) {
      qName = "@" + parentDir + "/" + FilenameUtils.removeExtension(name);
    }

    URI uri = new URI(false, uriFilePath);

    ElementNode root =
        new ElementNode(GraphUtil.nid(), Language.XML, type("file", true), "", name, qName, uri);
    stack.push(root);
    GraphUtil.addNode(root);
    logger.debug("Start Parsing {}", uriFilePath);
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
    URI xllUri = new URI(false, uriFilePath);
    Layer layer = xllUri.addLayer(idtf, Language.XML);

    // qname = tag/type name, name = identifier
    ElementNode en = new ElementNode(GraphUtil.nid(), Language.XML, nType, "", "", "", xllUri);
    pathMap.put(en, idtf);
    // TODO correctly set the start line with locator stack
    en.setRange(
        new Range(
            startElePosition.getStartLine(),
            locator.getLineNumber(),
            startElePosition.getStartColumn() - 1,
            locator.getColumnNumber() - 1));
    stack.push(en);

    int lineToStartSearch = startElePosition.getStartLine() - 1;

    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        String key = attributes.getQName(i);
        String value = attributes.getValue(i);

        int keyLine = -1, keyColumn = -1;
        int valueLine = -1, valueColumn = -1;

        for (int j = lineToStartSearch; j < locator.getLineNumber(); j++) {
          if (keyLine == -1 && keyColumn == -1) {
            int index = sourceFile.get(j).indexOf(key);
            if (index >= 0) {
              keyLine = j + 1;
              keyColumn = index;
            }
          }
          if (keyLine > 0 && keyColumn > 0) {
            int index = sourceFile.get(j).indexOf(value);
            if (index >= 0) {
              valueLine = j + 1;
              valueColumn = index;
            }
          }
        }

        // each attribute should only be processed once
        // either for def, or for ref
        // definitions (id)
        int endColumn = valueLine != -1 ? valueColumn + value.length() - 1 : valueColumn;
        if ("name".equals(key)) {
          // for resources
          // ref in java: R.qname.value
          // ref in xml: @qname/value
          String resName = "@" + qName + "/" + value;
          layer.setIdentifier(idtf + "/name");
          en.setName(value);
          en.setQualifiedName(resName);
          en.setRange(new Range(valueLine, valueLine, valueColumn, endColumn));

          xllUri.addLayer(URI.checkInvalidCh(value));
          defPool.put(resName, en);
        } else if ("android:id".equals(key) && value.startsWith("@+")) {
          // fr components
          en.setName(value);
          layer.setIdentifier(idtf + "/" + URI.checkInvalidCh("android:id"));
          String identifier = value.replace("+", "");
          en.setQualifiedName(identifier);
          en.setRange(new Range(valueLine, valueLine, valueColumn, endColumn));

          xllUri.addLayer(URI.checkInvalidCh(value));
          defPool.put(identifier, en);
        } else if ("id".equals(key)) {
          // fr components
          en.setName(value);
          layer.setIdentifier(idtf + "/" + URI.checkInvalidCh("id"));
          en.setQualifiedName(value);
          en.setRange(new Range(valueLine, valueLine, valueColumn, endColumn));

          xllUri.addLayer(URI.checkInvalidCh(value));
          defPool.put(value, en);
        } else {
          // references
          if (value.startsWith("@")) {
            URI refURI = new URI(false, uriFilePath);
            refURI.addLayer(idtf + "/" + URI.checkInvalidCh(key), Language.XML);
            refURI.addLayer(URI.checkInvalidCh(value), Language.ANY);

            Type eType = type(key);
            RelationNode rn =
                new RelationNode(GraphUtil.nid(), Language.XML, eType, key + "=" + value);
            rn.setUri(refURI);
            // TODO correctly set the start line with locator stack
            rn.setRange(new Range(valueLine, valueLine, valueColumn, endColumn));
            GraphUtil.addNode(rn);

            // unified references (may should use regex for matching)
            usePool.add(Triple.of(rn, eType, value));
          }
        }
      }
    }

    GraphUtil.addNode(en);
    super.startElement(uri, localName, qName, attributes);
    // Keep snapshot of start location, for later when end of element is found.
    locatorStack.push(new LocatorImpl(locator));

    //    updateStartElePostion(locator);
  }

  private void updateStartElePostion(Locator locator) {
    int endLine = locator.getLineNumber(), endColumn = locator.getColumnNumber();
    if (endLine > startElePosition.getStartLine()) {
      startElePosition.setStartPosition(endLine, endColumn);
    } else if (endLine == startElePosition.getStartLine()
        && endColumn > startElePosition.getStartColumn()) {
      startElePosition.setStartPosition(endLine, endColumn);
    }
  }
}
