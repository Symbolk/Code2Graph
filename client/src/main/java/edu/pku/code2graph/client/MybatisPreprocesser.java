package edu.pku.code2graph.client;

import edu.pku.code2graph.gen.xml.MybatisMapperHandler;
import edu.pku.code2graph.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MybatisPreprocesser {
  private static MybatisMapperHandler handler = new MybatisMapperHandler();

  public static void preprocessMapperXmlFile(String repoDir) throws ParserConfigurationException, SAXException {
    List<String> filePaths = FileUtil.listFilePaths(repoDir, ".xml");

    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser parser = factory.newSAXParser();
    for (String filePath : filePaths) {
      File file = new File(filePath);
      handler.setFilePath(FilenameUtils.separatorsToUnix(filePath));
      try {
        parser.parse(file, handler);
      } catch (SAXException | IOException e) {
        System.out.println(filePath);
      }
    }
  }

  public static MybatisMapperHandler getHandler() {
    return handler;
  }
}
