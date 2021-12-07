package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.Graph;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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

      handler.buildEdges();
    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
    }
    return handler.getGraph();
  }
}
