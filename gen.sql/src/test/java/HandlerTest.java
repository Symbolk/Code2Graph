import edu.pku.code2graph.gen.sql.SqlParser;
import edu.pku.code2graph.gen.sql.StatementHandler;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class HandlerTest {
  private static List<String> filePaths = new ArrayList<>();

  @BeforeAll
  public static void setUpBeforeAll() {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

    filePaths.add("src/test/resources/test.sql");
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
  }

  @Test
  public void testAlter() {
    String sql = "ALTER TABLE Customers\n" + "ADD Email varchar(255);";
    SqlParser parser = new SqlParser();
    StatementHandler hdl = new StatementHandler();
    hdl.generateFrom(parser.parseLines(sql));
    GraphVizExporter.printAsDot(GraphUtil.getGraph());
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
  }

  @Test
  public void testWhere() {
    String sql = "SELECT * FROM Customers\n" + "WHERE Country='Germany' AND City='Berlin';";
    SqlParser parser = new SqlParser();
    StatementHandler hdl = new StatementHandler();
    hdl.generateFrom(parser.parseLines(sql));
    GraphVizExporter.printAsDot(GraphUtil.getGraph());
  }
}
