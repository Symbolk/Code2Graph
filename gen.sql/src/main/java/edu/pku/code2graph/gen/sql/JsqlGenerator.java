package edu.pku.code2graph.gen.sql;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.Edge;
import edu.pku.code2graph.model.Node;
import net.sf.jsqlparser.statement.Statements;
import org.jgrapht.Graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Register(id = "sql-jsql", accept = "\\.sql", priority = Registry.Priority.MAXIMUM)
public class JsqlGenerator extends Generator {
  private SqlParser parser = new SqlParser();

  @Override
  protected Graph<Node, Edge> generate(List<String> filePaths) {
    List<Statements> stmtsList = new ArrayList<>();
    StatementHandler hdl = new StatementHandler();
    filePaths.forEach(
        (file) -> {
          try {
            Statements stmts = parser.parseFile(file);
            hdl.generateFrom(stmts);
            stmtsList.add(stmts);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
    return null;
  }
}
