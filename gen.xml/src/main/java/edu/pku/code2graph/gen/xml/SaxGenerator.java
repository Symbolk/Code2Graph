package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import org.jgrapht.Graph;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Register(id = "xml-sax", accept = "\\.xml$", priority = Registry.Priority.MAXIMUM)
public class SaxGenerator extends Generator {
  @Override
  protected Graph<Node, Edge> generate(List<String> filePaths) {
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser parser = factory.newSAXParser();
      String path = "gen.xml/src/test/resources/layout.xml";

      // category files：resources, mainfest, layout
      //      for(String path : filePaths) {
      //
      //      }
      // assign specific handler, maybe use the factory pattern?

      // parse while getting the graph
      //      for(){
      File f = new File(path);
      LayoutHandler dh = new LayoutHandler();
      parser.parse(f, dh);
      //      }

    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
    }
    return null;
  }
}
