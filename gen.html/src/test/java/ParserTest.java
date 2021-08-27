import edu.pku.code2graph.gen.html.DocumentHandler;
import edu.pku.code2graph.gen.html.HtmlParser;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {

  @BeforeAll
  public static void setUpBeforeAll() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
  }

  @Test
  public void testString() {
    String dom =
        "<html><head><title>First parse</title></head>"
            + "<body><p>Parsed HTML into a doc.</p></body></html>";
    HtmlParser parser = new HtmlParser();
    Document doc = parser.parseString(dom);
    System.out.println(doc);
  }

  @Test
  public void testInvalidCh() {
    DocumentHandler hdl = new DocumentHandler();
    assertThat(URI.checkInvalidCh("he@an/d*i:and[1]")).isEqualTo("he\\@an\\/d\\*i\\:and\\[1\\]");
  }

  @Test
  public void testHandler() {
    DocumentHandler hdl = new DocumentHandler();
    String dom =
        "<html><head><title fontsize=16px>First parse</title></head>"
            + "<body><p>Parsed HTML into a doc.</p></body></html>";
    HtmlParser parser = new HtmlParser();
    Document doc = parser.parseString(dom);
    hdl.setFilePath("src/a.html");
    hdl.generateFromDoc(doc);
    GraphVizExporter.printAsDot(GraphUtil.getGraph());
  }
}
