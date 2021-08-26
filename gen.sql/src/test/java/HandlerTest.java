import edu.pku.code2graph.gen.sql.JsqlGenerator;
import edu.pku.code2graph.gen.sql.SqlParser;
import edu.pku.code2graph.gen.sql.StatementHandler;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.Graph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HandlerTest {
  private static List<String> filePaths = new ArrayList<>();

  @BeforeAll
  public static void setUpBeforeAll() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

    filePaths.add("src/test/resources/test.sql");
    filePaths.add("src/test/resources/simple.sql");
  }

  @Test
  public void testUpdate() {
    String sql =
        "UPDATE Customers\n"
            + "SET ContactName = 'Alfred Schmidt', City= 'Frankfurt'\n"
            + "WHERE CustomerID = 1;";
    SqlParser parser = new SqlParser();
    StatementHandler hdl = new StatementHandler();
    hdl.generateFrom(parser.parseLines(sql));
    GraphVizExporter.printAsDot(GraphUtil.getGraph());

    String testVar = "CustomerID";
    String sqlLine3 = "WHERE CustomerID = 1;";
    GraphUtil.getGraph()
        .vertexSet()
        .forEach(
            v -> {
              if (v instanceof ElementNode && ((ElementNode) v).getName().equals(testVar)) {
                assertThat(v.getRange().getStartLine()).isEqualTo(2);
                assertThat(v.getRange().getStartColumn()).isEqualTo(sqlLine3.indexOf(testVar));
                assertThat(v.getRange().getEndLine()).isEqualTo(2);
                assertThat(v.getRange().getEndColumn())
                    .isEqualTo(sqlLine3.indexOf(testVar) + testVar.length() - 1);
                assertThat(((ElementNode) v).getUri().getIdentifier())
                    .isEqualTo("Update/Where/=/CustomerID");
              }
            });
  }

  @Test
  public void testAlter() {
    String sql = "ALTER TABLE Customers\n" + "ADD Email varchar(255);";
    SqlParser parser = new SqlParser();
    StatementHandler hdl = new StatementHandler();
    hdl.generateFrom(parser.parseLines(sql));
    GraphVizExporter.printAsDot(GraphUtil.getGraph());

    String testVar = "Email";
    String sqlLine2 = "ADD Email varchar(255);";
    GraphUtil.getGraph()
        .vertexSet()
        .forEach(
            v -> {
              if (v instanceof ElementNode && ((ElementNode) v).getName().equals(testVar)) {
                assertThat(((ElementNode) v).getUri().getIdentifier()).isEqualTo("Alter/Email");
              }
            });
  }

  @Test
  public void testInsert() {
    String sql =
        "INSERT INTO Customers (CustomerName, ContactName, Address, City, PostalCode, Country)\n"
            + "VALUES ('Cardinal', 'Tom B. Erichsen', 'Skagen 21', 'Stavanger', '4006', 'Norway');";
    SqlParser parser = new SqlParser();
    StatementHandler hdl = new StatementHandler();
    hdl.generateFrom(parser.parseLines(sql));
    GraphVizExporter.printAsDot(GraphUtil.getGraph());

    String testVar = "CustomerName";
    GraphUtil.getGraph()
        .vertexSet()
        .forEach(
            v -> {
              if (v instanceof ElementNode && ((ElementNode) v).getName().equals(testVar)) {
                assertThat(v.getRange().getStartLine()).isEqualTo(0);
                assertThat(v.getRange().getStartColumn()).isEqualTo(sql.indexOf(testVar));
                assertThat(v.getRange().getEndLine()).isEqualTo(0);
                assertThat(v.getRange().getEndColumn())
                    .isEqualTo(sql.indexOf(testVar) + testVar.length() - 1);
              }
            });
  }

  @Test
  public void testWhere() {
    String sql = "SELECT * FROM Customers\n" + "WHERE Country='Germany' AND City='Berlin';";
    SqlParser parser = new SqlParser();
    StatementHandler hdl = new StatementHandler();
    hdl.generateFrom(parser.parseLines(sql));
    GraphVizExporter.printAsDot(GraphUtil.getGraph());

    List<RelationNode> whereNode = new ArrayList<>();
    GraphUtil.getGraph()
        .vertexSet()
        .forEach(
            v -> {
              if (v instanceof RelationNode && ((RelationNode) v).getSymbol().equals("Where")) {
                whereNode.add((RelationNode) v);
              }
            });
    assertThat(whereNode.size()).isEqualTo(1);
  }

  @Test
  public void testGenerator() throws IOException {
    JsqlGenerator generator = new JsqlGenerator();
    GraphUtil.clearGraph();
    generator.generateFrom().files(filePaths);
    Graph<Node, Edge> graph = GraphUtil.getGraph();
    GraphVizExporter.printAsDot(graph);
    Map<String, Boolean> inVertex = new HashMap<>();
    graph
        .vertexSet()
        .forEach(
            v -> {
              if (v instanceof ElementNode) {
                String filename = ((ElementNode) v).getUri().getFile();
                assertThat(filename).isIn(filePaths);
                inVertex.put(filename, true);
              }
            });
    assertThat(filePaths.size()).isEqualTo(inVertex.size());
  }

  @Test
  public void testInline() {
    JsqlGenerator generator = new JsqlGenerator();
    GraphUtil.clearGraph();
    String query = "SELECT * FROM Customers\n" + "WHERE Country='Germany' AND City='Berlin';";
    String idtf = "Class/function/Select";
    Language lang = Language.JAVA;
    String filepath = "src/resources/test/what.java";
    generator.generate(
        query, filepath, lang, idtf);

    List<ElementNode> countryNode = new ArrayList<>();
    GraphUtil.getGraph()
            .vertexSet()
            .forEach(
                    v -> {
                      if (v instanceof ElementNode && ((ElementNode) v).getName().equals("Country")) {
                        countryNode.add((ElementNode) v);
                      }
                    });
    countryNode.forEach(node->{
      assertThat(node.getUri().getIdentifier()).isEqualTo(idtf);
      assertThat(node.getUri().getFile()).isEqualTo(filepath);
      assertThat(node.getUri().getLang()).isEqualTo(lang);
      assertThat(node.getUri().getInline().getIdentifier()).isEqualTo("Select/Where/=/Country");
    });
  }
}
