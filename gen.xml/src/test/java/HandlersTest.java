import edu.pku.code2graph.gen.xml.AbstractHandler;
import edu.pku.code2graph.gen.xml.AndroidHandler;
import edu.pku.code2graph.gen.xml.SaxGenerator;
import edu.pku.code2graph.gen.xml.TestDemo;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.ElementNode;
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
    //    filePaths.add("src/test/resources/strings.xml");
    //    filePaths.add("src/test/resources/manifest.xml");
  }

  @Test
  public void testHandler() throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser parser = factory.newSAXParser();
    String filePath = "src/test/resources/layout.xml";
    File f = new File(filePath);
    AbstractHandler dh = new AndroidHandler();
    dh.setFilePath(filePath);
    parser.parse(f, dh);
    Graph<Node, Edge> graph = GraphUtil.getGraph();
    GraphVizExporter.printAsDot(graph);

    for (Node node : graph.vertexSet()) {
      if (node instanceof ElementNode) {
        switch (((ElementNode) node).getName()) {
          case "@+id/discovery_toolbar":
            assertThat(((ElementNode) node).getUri().getIdentifier())
                .isEqualTo("com.kickstarter.ui.views.DiscoveryToolbar/android\\:id");
            assertThat(((ElementNode) node).getUri().getInlineIdentifier())
                .isEqualTo("\\@+id\\/discovery_toolbar");
            break;
          case "@+id/filter_text_view":
            assertThat(((ElementNode) node).getUri().getIdentifier())
                .isEqualTo(
                    "com.kickstarter.ui.views.DiscoveryToolbar/RelativeLayout/LinearLayout/TextView/android\\:id");
            assertThat(((ElementNode) node).getUri().getInlineIdentifier())
                .isEqualTo("\\@+id\\/filter_text_view");
            break;
        }
      }
    }
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
