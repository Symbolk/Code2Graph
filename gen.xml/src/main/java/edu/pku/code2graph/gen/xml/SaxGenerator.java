package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.gen.xml.handlers.LayoutHandler;
import edu.pku.code2graph.gen.xml.handlers.ManifestHandler;
import edu.pku.code2graph.gen.xml.handlers.ResourceHandler;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Register(id = "xml-sax", accept = "\\.xml$", priority = Registry.Priority.MAXIMUM)
public class SaxGenerator extends Generator {
  private List<String> supportedTypes = Arrays.asList("resources", "manifest", "layout");

  @Override
  protected Graph<Node, Edge> generate(List<String> filePaths) {
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser parser = factory.newSAXParser();

      Map<String, List<String>> typeToPaths = categorizeFiles(filePaths);
      // category files：resources, manifest, layout
      for (Map.Entry<String, List<String>> entry : typeToPaths.entrySet()) {
        if ("resources".equals(entry.getKey())) {
          handle(entry.getValue(), parser, new ResourceHandler());
        } else if ("manifest".equals(entry.getKey())) {
          handle(entry.getValue(), parser, new ManifestHandler());
        } else if ("layout".equals(entry.getKey())) {
          handle(entry.getValue(), parser, new LayoutHandler());
        }
      }
    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void handle(List<String> filePaths, SAXParser parser, AbstractHandler handler)
      throws IOException, SAXException {
    for (String filePath : filePaths) {
      parser.parse(new File(filePath), handler);
    }
  }

  public Map<String, List<String>> categorizeFiles(List<String> filePaths) {
    Map<String, List<String>> typeToPaths = new HashMap<>();
    supportedTypes.forEach(type -> typeToPaths.put(type, new ArrayList<>()));

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    try {

      DocumentBuilder builder = factory.newDocumentBuilder();
      for (String filePath : filePaths) {
        Document doc = builder.parse(filePath);
        boolean matched = false;
        for (String type : supportedTypes) {
          if (checkIfNodeExists(doc, "//" + type)) {
            typeToPaths.get(type).add(filePath);
            matched = true;
            break;
          }
        }
        if (!matched) {
          typeToPaths.get(supportedTypes.get(supportedTypes.size() - 1)).add(filePath);
        }
      }
    } catch (Exception e) {
      logger.error("Exception when parsing: " + filePaths);
      e.printStackTrace();
    }
    return typeToPaths;
  }

  private boolean checkIfNodeExists(Document document, String xpathExpression) {
    boolean matched = false;

    // Create XPathFactory object
    XPathFactory xpathFactory = XPathFactory.newInstance();

    // Create XPath object
    XPath xpath = xpathFactory.newXPath();

    try {
      // Create XPathExpression object
      XPathExpression expr = xpath.compile(xpathExpression);

      // Evaluate expression result on XML document
      NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);

      if (nodes != null && nodes.getLength() > 0) {
        matched = true;
      }

    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }
    return matched;
  }

  public List<String> getSupportedTypes() {
    return supportedTypes;
  }
}
