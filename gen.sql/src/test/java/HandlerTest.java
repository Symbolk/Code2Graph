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
  public void testSelect(){
    String sql = "SELECT a, b from tab";
    SqlParser parser = new SqlParser();
    StatementHandler hdl = new StatementHandler();
    hdl.generateFrom(parser.parseLines(sql));
    GraphVizExporter.printAsDot(GraphUtil.getGraph());
  }
}
