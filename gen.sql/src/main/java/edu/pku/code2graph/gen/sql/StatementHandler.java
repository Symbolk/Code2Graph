package edu.pku.code2graph.gen.sql;

import edu.pku.code2graph.gen.sql.model.NodeType;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.GraphUtil;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Block;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.pku.code2graph.model.TypeSet.type;

public class StatementHandler {
  protected Logger logger = LoggerFactory.getLogger(StatementHandler.class);

  protected Graph<Node, Edge> graph = GraphUtil.getGraph();

  // temporarily save the current file path here
  protected String filePath;

  protected Statement current = null;

  public void generateFrom(Statements stmts) {
    List<Statement> listOfStmt = stmts.getStatements();
    listOfStmt.forEach(
        (stmt) -> {
          current = stmt;
          clearForNextStatement();
          tablesNamesFinder.getTableList(stmt);
          //          stmt.accept(deParser);
        });
  }

  //  private Stack<Tuple2<Statement, SimpleNode>> rootNode;
  private SimpleNode parentNode = null;
  private Map<SimpleNode, Node> nodePool = new HashMap<>();
  private Map<Statement, Node> rootNodePool = new HashMap<>();

  public static final Type CHILD = type("child");

  private TablesNamesFinder tablesNamesFinder =
      new TablesNamesFinder() {
        @Override
        public void visit(SubSelect subSelect) {
          logger.debug("subselect " + subSelect.toString());
          super.visit(subSelect);
        }

        @Override
        public void visit(PlainSelect el) {
          logger.debug("plainselect " + el.toString());
          Type nodeType = NodeType.PlainSelect;
          SimpleNode snode = el.getASTNode();
          RelationNode rn =
              new RelationNode(GraphUtil.nid(), Language.SQL, nodeType, el.toString(), "SELECT");
          rn.setRange(getRange(snode));
          graph.addVertex(rn);
          nodePool.put(snode, rn);
          findParentEdge(snode, rn);
          super.visit(el);
        }

        @Override
        public void visit(Update el) {
          logger.debug("update " + el.toString());
          Type nodeType = NodeType.Update;
          RelationNode rn =
              new RelationNode(GraphUtil.nid(), Language.SQL, nodeType, el.toString(), "UPDATE");
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          while (recur.jjtGetParent() != null) {
            recur = (SimpleNode) recur.jjtGetParent();
          }
          rn.setRange(getRange(recur));
          graph.addVertex(rn);
          rootNodePool.put(el, rn);
          super.visit(el);
        }

        @Override
        public void visit(Insert el) {
          logger.debug("insert " + el.toString());
          Type nodeType = NodeType.Insert;
          RelationNode rn =
              new RelationNode(GraphUtil.nid(), Language.SQL, nodeType, el.toString(), "INSERT");
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          while (recur.jjtGetParent() != null) {
            recur = (SimpleNode) recur.jjtGetParent();
          }
          rn.setRange(getRange(recur));
          graph.addVertex(rn);
          rootNodePool.put(el, rn);
          super.visit(el);
        }

        @Override
        public void visit(Column el) {
          logger.debug("column " + el.toString());
          Type nodeType = NodeType.Column;
          ElementNode en =
              new ElementNode(
                  GraphUtil.nid(),
                  Language.SQL,
                  nodeType,
                  el.toString(),
                  el.getColumnName(),
                  el.getFullyQualifiedName());
          SimpleNode snode = el.getASTNode();
          en.setRange(getRange(snode));
          graph.addVertex(en);
          nodePool.put(snode, en);
          findParentEdge(snode, en);
          super.visit(el);
        }

        @Override
        public void visit(Table el) {
          logger.debug("table " + el.toString());
          Type nodeType = NodeType.Table;
          ElementNode en =
              new ElementNode(
                  GraphUtil.nid(),
                  Language.SQL,
                  nodeType,
                  el.toString(),
                  el.getName(),
                  el.getFullyQualifiedName());
          SimpleNode snode = el.getASTNode();
          en.setRange(getRange(snode));
          graph.addVertex(en);
          nodePool.put(snode, en);
          findParentEdge(snode, en);
          super.visit(el);
        }

        //        @Override
        //        public void visit(AllTableColumns el){
        //          logger.debug("btw:", el.toString());
        //          super.visit(el);
        //        }
      };

  private StatementDeParser deParser =
      new StatementDeParser(new StringBuilder()) {
        @Override
        public void visit(Block subSelect) {
          logger.debug("stmt " + subSelect.toString());
          super.visit(subSelect);
        }
      };

  private void findParentEdge(SimpleNode snode, Node node) {
    parentNode = (SimpleNode) snode.jjtGetParent();
    boolean found = false;
    while (parentNode != null) {
      if (nodePool.get(parentNode) != null) {
        graph.addEdge(nodePool.get(parentNode), node, new Edge(GraphUtil.eid(), CHILD));
        logger.debug("found parent");
        found = true;
        break;
      }
      parentNode = (SimpleNode) parentNode.jjtGetParent();
    }
    if (!found) {
      rootNodePool.forEach(
          (statement, node1) -> graph.addEdge(node1, node, new Edge(GraphUtil.eid(), CHILD)));
    }
  }

  private Range getRange(SimpleNode snode) {
    return new Range(
        snode.jjtGetFirstToken().beginLine - 1,
        snode.jjtGetLastToken().endLine - 1,
        snode.jjtGetFirstToken().beginColumn - 1,
        snode.jjtGetLastToken().endColumn - 1);
  }

  private void clearForNextStatement() {
    nodePool.clear();
    rootNodePool.clear();
  }

  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }
}
