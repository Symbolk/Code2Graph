package edu.pku.code2graph.gen.sql;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
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
            hdl.setFilePath(file);
            hdl.generateFrom(stmts);
            stmtsList.add(stmts);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
    GraphUtil.getUriMap().put(Language.SQL, hdl.getUriMap());
    return hdl.getGraph();
  }

  public Graph<Node, Edge> generate(
      String query, String filepath, Language lang, String identifier) {
    StatementHandler hdl = new StatementHandler(true, lang, identifier);
    Statements stmt = parser.parseLines(query);
    hdl.setFilePath(filepath);
    hdl.generateFrom(stmt);
    GraphUtil.getUriMap().put(Language.SQL, hdl.getUriMap());
    return hdl.getGraph();
  }
}
