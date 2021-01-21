import edu.pku.code2graph.gen.xml.LayoutHandler;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;

public class LayoutTest {
  @BeforeAll
  public static void setUpBeforeAll() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
  }

  @Test
  public void testLayout() throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser parser = factory.newSAXParser();
    String path = "src/test/resources/layout.xml";
    File f = new File(path);
    LayoutHandler dh = new LayoutHandler();
    parser.parse(f, dh);

    GraphVizExporter.printAsDot(GraphUtil.getGraph());
  }
}
