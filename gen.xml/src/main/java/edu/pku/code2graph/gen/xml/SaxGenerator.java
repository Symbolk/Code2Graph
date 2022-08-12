package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.*;
import org.apache.commons.io.FilenameUtils;
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
    AbstractHandler handler = new AndroidHandler();
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser parser = factory.newSAXParser();
      for (String filePath : filePaths) {
        File file = new File(filePath);
        handler.setFilePath(FilenameUtils.separatorsToUnix(filePath));
        try {
          parser.parse(file, handler);
        } catch (SAXException e) {
          logger.warn("can't initialize parser correctly for " + filePath);
        }
      }

    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
    }
    return handler.getGraph();
  }
}
