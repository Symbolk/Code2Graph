package edu.pku.code2graph.gen.sql;

import edu.pku.code2graph.gen.Generator;
import edu.pku.code2graph.gen.Register;
import edu.pku.code2graph.gen.Registry;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import net.sf.jsqlparser.statement.Statements;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.Graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Register(id = "sql-jsql", accept = "\\.sql", priority = Registry.Priority.MAXIMUM)
public class JsqlGenerator extends Generator {
  private SqlParser parser = new SqlParser();

  private StatementHandler hdl = null;

  @Override
  protected Graph<Node, Edge> generate(List<String> filePaths) {
    List<Statements> stmtsList = new ArrayList<>();
    hdl = new StatementHandler();
    filePaths.forEach(
        (filePath) -> {
          try {
            Statements stmts = parser.parseFile(filePath);
            hdl.setFilePath(FilenameUtils.separatorsToUnix(filePath));
            if (stmts != null) {
              hdl.generateFrom(stmts, null);
              stmtsList.add(stmts);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
    return hdl.getGraph();
  }

  // queryId: for mybatis xml mapper, be null in other scene
  public Graph<Node, Edge> generate(
      String query,
      String filepath,
      Language lang,
      String identifier,
      URI parentURI,
      String queryId) {
    Graph<Node, Edge> graph = GraphUtil.getGraph();
    String newQuery = addQuotesToQuery(query);
    hdl = new StatementHandler(true, lang, identifier, parentURI);
    Statements stmt = parser.parseLines(newQuery);
    hdl.setFilePath(filepath);
    if (stmt != null) hdl.generateFrom(stmt, queryId);
    //    for (Node n : hdl.getGraph().vertexSet()) {
    //      if (n instanceof ElementNode) {
    //        ElementNode en = (ElementNode) n;
    //        // TODO: set correnct range in case of mybatis parameter elements
    //        if (en.getName().startsWith("\"#{") || en.getName().startsWith("\"${")) {
    //          String name = en.getName();
    //          String qn = en.getQualifiedName();
    //          en.setName(name.substring(1, name.length() - 1));
    //          en.setQualifiedName(qn.substring(1, qn.length() - 1));
    //        }
    //      }
    //    }
    return hdl.getGraph();
  }

  public Map<String, List<ElementNode>> getIdentifiers() {
    if (hdl == null) return null;
    return hdl.idToIdentifierEn;
  }

  public void clearIdentifiers() {
    if (hdl == null) return;
    hdl.idToIdentifierEn.clear();
  }

  public Map<String, List<RelationNode>> getQueries() {
    if (hdl == null) return null;
    return hdl.idToQueryRn;
  }

  public void clearQueries() {
    if (hdl == null) return;
    hdl.idToQueryRn.clear();
  }

  private List<String> paramMark = Arrays.asList("#{", "${");

  private String addQuotesToQuery(String query) {
    String res = query;
    int resIdx = 0;
    for (int i = 0; i < query.length(); i++) {
      if (i + 1 < query.length() && paramMark.contains(query.substring(i, i + 2))) {
        res = res.substring(0, resIdx) + "\"" + res.substring(resIdx);

        i++;
        resIdx += 2;
      } else if (query.charAt(i) == '}') {
        res = res.substring(0, resIdx + 1) + "\"" + res.substring(resIdx + 1);

        resIdx++;
      }
      resIdx++;
    }
    return res;
  }
}
