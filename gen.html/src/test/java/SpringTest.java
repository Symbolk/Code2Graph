import edu.pku.code2graph.gen.html.HtmlParser;
import edu.pku.code2graph.gen.html.JsoupGenerator;
import edu.pku.code2graph.gen.html.SpringHandler;
import edu.pku.code2graph.gen.html.StandardDialectParser;
import edu.pku.code2graph.gen.html.model.DialectNode;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.Graph;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringTest {
  private static List<String> filePaths = new ArrayList<>();

  @BeforeAll
  public static void setUpBeforeAll() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

    filePaths.add("src/test/resources/simple.html");
    filePaths.add("src/test/resources/member.html");
  }

  @Test
  public void testParserWithWholeVariable() {
    StandardDialectParser parser = new StandardDialectParser();
    String code = "#{header.address.city}";
    DialectNode node = parser.parseTree(code);
    assertThat(node.getName()).isEqualTo("header.address.city");
    assertThat(node.getStartIdx()).isEqualTo(0);
    assertThat(node.getEndIdx()).isEqualTo(code.length() - 1);
  }

  @Test
  public void testParserWithPartVariable() {
    StandardDialectParser parser = new StandardDialectParser();
    String code = "book : ${ book }";
    DialectNode node = parser.parseTree(code);
    assertThat(node.getName()).isEqualTo("book");
    assertThat(node.getStartIdx()).isEqualTo(code.indexOf("$"));
    assertThat(node.getEndIdx()).isEqualTo(code.length() - 1);
  }

  @Test
  public void testHadler() {
    HtmlParser parser = new HtmlParser();
    SpringHandler hdl = new SpringHandler();
    String dom =
        "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head>\n"
            + "    <title>Admin | Projects</title>\n"
            + "</head>\n"
            + "<body>\n"
            + "    <tr data-th-each=\"${projects}\">\n"
            + "        <td><a data-th-href=\"@{${project.id}}\" data-th-text=\"${project.name}\">spring-framework</a></td>\n"
            + "    </tr>\n"
            + "</body>";
    Document doc = parser.parseString(dom);
    hdl.generateFromDoc(doc);
    GraphVizExporter.printAsDot(GraphUtil.getGraph());
  }

  @Test
  public void testGenerator() throws IOException {
    JsoupGenerator generator = new JsoupGenerator();
    Graph<Node, Edge> graph = generator.generateFrom().files(filePaths);
    GraphVizExporter.printAsDot(graph);
  }
}
