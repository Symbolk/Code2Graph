package edu.pku.code2graph.gen.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxHandler extends DefaultHandler {

  // text content between tag start and end
  private String tempString;

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    tempString = new String(ch, start, length);
    System.out.print(tempString);
    super.characters(ch, start, length);
  }

  @Override
  public void endDocument() throws SAXException {
    System.out.println("\nEnd Parsing");
    super.endDocument();
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    System.out.print("</");
    System.out.print(qName);
    System.out.print(">");
    super.endElement(uri, localName, qName);
  }

  @Override
  public void startDocument() throws SAXException {
    System.out.println("Start Parsing");
    super.startDocument();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {

    System.out.print("<");
    System.out.print(qName);

    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        if ("android:id".equals(attributes.getQName(i))) {
          System.out.print(" " + attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"");
        }
      }
    }
    System.out.print(">");
    super.startElement(uri, localName, qName, attributes);
  }
}
