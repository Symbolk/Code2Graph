import edu.pku.code2graph.gen.xml.DefaultHandler;
import edu.pku.code2graph.gen.xml.SaxGenerator;
import edu.pku.code2graph.gen.xml.TestDemo;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.Graph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HandlersTest {
  private static List<String> filePaths = new ArrayList<>();

  @BeforeAll
  public static void setUpBeforeAll() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

    filePaths.add("src/test/resources/layout.xml");
    filePaths.add("src/test/resources/strings.xml");
    filePaths.add("src/test/resources/manifest.xml");
  }

  @Test
  public void testClassifier() {
    TestDemo demo = new TestDemo();
    Map<String, List<String>> typeToPaths = demo.categorizeFiles(filePaths);
    for (Map.Entry<String, List<String>> entry : typeToPaths.entrySet()) {
      assertThat(entry.getValue().size()).isEqualTo(1);
    }
  }

  @Test
  public void testHandler() throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser parser = factory.newSAXParser();

    TestDemo demo = new TestDemo();
    Map<String, List<String>> typeToPaths = demo.categorizeFiles(filePaths);

    File f = new File(typeToPaths.get("layout").get(0));
    DefaultHandler dh = new DefaultHandler();
    parser.parse(f, dh);
    GraphVizExporter.printAsDot(GraphUtil.getGraph());
  }

  @Test
  public void testGenerator() {
    SaxGenerator generator = new SaxGenerator();
    try {
      Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
      GraphVizExporter.printAsDot(graph);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
