package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.util.FileUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.File;

public class TestDemo {

  public static void main(String[] args) throws Exception {

    String path = "gen.xml/src/test/resources/layout.xml";
    System.out.println("-----------------------");

    // DOM way
    DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
    //    fac.setNamespaceAware(true);
    //    fac.setIgnoringElementContentWhitespace(true);
    DocumentBuilder builder = fac.newDocumentBuilder();

    // Load the input XML document, parse it and return an instance of the
    // Document class.
    Document document = builder.parse(new File(path));
    NodeList nodeList = document.getDocumentElement().getChildNodes();
    NodeList nodeList1 = document.getElementsByTagName("*Layout");
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        System.out.println(node.getNodeName() + node.getTextContent());
        System.out.println(node.getAttributes());
      }
    }

    System.out.println("-----------------------");

    String doc = FileUtil.readFileToString(path);
    ByteArrayInputStream is = new ByteArrayInputStream(doc.getBytes());
    XMLInputFactory xif = XMLInputFactory.newFactory();
    xif.setProperty(javax.xml.stream.XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    XMLStreamReader xr = xif.createXMLStreamReader(is);

    while (xr.hasNext()) {
      int t = xr.getEventType();
      switch (t) {
        case XMLEvent.ENTITY_REFERENCE:
          System.out.println("Entity: " + xr.getLocalName());
          break;
        case XMLEvent.START_DOCUMENT:
          System.out.println("Start Document");
          break;
        case XMLEvent.START_ELEMENT:
          System.out.println("Start Element: " + xr.getLocalName());
          break;
        case XMLEvent.END_DOCUMENT:
          System.out.println("End Document");
          break;
        case XMLEvent.END_ELEMENT:
          System.out.println("End Element: " + xr.getLocalName());
          break;
        default:
          System.out.println("Other:  ");
          break;
      }
      xr.next();
    }
  }
}
