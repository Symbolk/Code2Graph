package edu.pku.code2graph.gen.sql;

import edu.pku.code2graph.gen.sql.model.NodeType;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.AlterView;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.pku.code2graph.model.TypeSet.type;

public class StatementHandler {
  protected Logger logger = LoggerFactory.getLogger(StatementHandler.class);

  protected Graph<Node, Edge> graph = GraphUtil.getGraph();

  private boolean isInline = false;
  private Language wrapLang;
  private String wrapIdentifier;

  // temporarily save the current file path here
  protected String filePath;
  protected String uriFilePath;

  public StatementHandler() {}

  public StatementHandler(boolean inline, Language lang, String identifier) {
    this.isInline = inline;
    this.wrapLang = lang;
    this.wrapIdentifier = identifier;
  }

  public void generateFrom(Statements stmts) {
    //    stmts.accept(deParser);
    tablesNamesFinder.visit(stmts);
  }

  //  private Stack<Tuple2<Statement, SimpleNode>> rootNode;
  private SimpleNode parentNode = null;
  private Map<SimpleNode, Node> nodePool = new HashMap<>();
  private Map<Node, String> identifierMap = new HashMap<>();
  private Map<Statement, Node> rootNodePool = new HashMap<>();

  private boolean inClause = false;
  private Node clauseNode = null;

  public static final Type CHILD = type("child");

  private List<String> mybatisParamMark = Arrays.asList("\"#{", "\"${");

  private TablesNamesFinder tablesNamesFinder =
      new TablesNamesFinder() {
        @Override
        public void visit(Column el) {
          SimpleNode snode = el.getASTNode();
          String name = el.getColumnName();
          String qn = el.getFullyQualifiedName();
          String snippet = el.toString();
          Type type = NodeType.Column;
          for (String mark : mybatisParamMark) {
            if (name.startsWith(mark)) {
              name = name.substring(1, name.length() - 1);
              qn = qn.substring(1, qn.length() - 1);
              snippet = snippet.substring(1, snippet.length() - 1);
              type = NodeType.Parameter;
              break;
            }
          }
          addElementNode(snippet, type, name, qn, snode);
          super.visit(el);
        }

        @Override
        public void visit(Table el) {
          SimpleNode snode = el.getASTNode();
          addElementNode(
              el.toString(), NodeType.Table, el.getName(), el.getFullyQualifiedName(), snode);
        }

        @Override
        public void visit(Statements stmts) {
          stmts
              .getStatements()
              .forEach(
                  stmt -> {
                    clearForNextStatement();
                    stmt.accept(this);
                  });
        }

        @Override
        public void visit(CreateIndex el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.CreateIndex, "CreateIndex", recur);
          el.accept(deParser);
        }

        @Override
        public void visit(CreateTable el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.CreateTable, "CreateTable", recur);
          super.visit(el);
        }

        @Override
        public void visit(CreateView el) {
          el.accept(deParser);
        }

        @Override
        public void visit(AlterView el) {
          SimpleNode recur = (SimpleNode) el.getView().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.AlterView, "AlterView", recur);
          visit(el.getView());
          el.accept(deParser);
        }

        @Override
        public void visit(Delete el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.Delete, "Delete", recur);
          visit(el.getTable());

          if (el.getUsingList() != null) {
            for (Table using : el.getUsingList()) {
              visit(using);
            }
          }

          if (el.getJoins() != null) {
            for (Join join : el.getJoins()) {
              SimpleNode snode = join.getASTNode();
              RelationNode rn =
                  addRelationNode(join.toString(), NodeType.Join, "Join", snode, true);
              inClause = true;
              clauseNode = rn;
              join.getRightItem().accept(this);
              inClause = false;
            }
          }

          if (el.getWhere() != null) {
            SimpleNode whereNode = el.getWhere().getASTNode();
            RelationNode rn =
                addRelationNode(whereNode.toString(), NodeType.Where, "Where", whereNode, true);
            inClause = true;
            clauseNode = rn;
            el.getWhere().accept(this);
            inClause = false;
          }
        }

        @Override
        public void visit(Drop el) {
          SimpleNode recur = (SimpleNode) el.getName().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.Drop, "Drop", recur);
          el.getName().accept(selectDeParser);
        }

        @Override
        public void visit(Insert el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.Insert, "Insert", recur);
          el.getTable().accept(selectDeParser);
          if (el.getColumns() != null) {
            el.getColumns().forEach(column -> column.accept(expreDeParser));
          }
          if (el.getItemsList() != null) {
            el.getItemsList().accept(expreDeParser);
          }
          if (el.getSelect() != null) {
            visit(el.getSelect());
          }
        }

        @Override
        public void visit(Replace el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.Replace, "Replace", recur);
          super.visit(el);
        }

        @Override
        public void visit(Select el) {
          SimpleNode snode = ((PlainSelect) el.getSelectBody()).getASTNode();
          RelationNode rn = addRelationNode(el.toString(), NodeType.Select, "Select", snode, true);
          rootNodePool.put(el, rn);
          super.visit(el);
        }

        @Override
        public void visit(PlainSelect el) {
          if (el.getSelectItems() != null) {
            for (SelectItem item : el.getSelectItems()) {
              item.accept(this);
            }
          }

          if (el.getFromItem() != null) {
            //            RelationNode rn =
            //                addRelationNode(el.getFromItem().toString(), NodeType.From, "From",
            // null, false);
            //            inClause = true;
            //            clauseNode = rn;
            el.getFromItem().accept(this);
            //            inClause = false;
          }

          if (el.getJoins() != null) {
            for (Join join : el.getJoins()) {
              SimpleNode joinNode = join.getASTNode();
              RelationNode joinRn =
                  addRelationNode(join.toString(), NodeType.Join, "Join", joinNode, true);
              inClause = true;
              clauseNode = joinRn;
              join.getRightItem().accept(this);
              inClause = false;
            }
          }
          if (el.getWhere() != null) {
            SimpleNode whereNode = el.getWhere().getASTNode();
            RelationNode whereRn =
                addRelationNode(el.getWhere().toString(), NodeType.Where, "Where", whereNode, true);
            inClause = true;
            clauseNode = whereRn;
            el.getWhere().accept(this);
            inClause = false;
          }

          if (el.getHaving() != null) {
            SimpleNode havingNode = el.getHaving().getASTNode();
            RelationNode havingRn =
                addRelationNode(
                    el.getHaving().toString(), NodeType.Having, "Having", havingNode, true);
            inClause = true;
            clauseNode = havingRn;
            el.getHaving().accept(this);
            inClause = false;
          }

          if (el.getOracleHierarchical() != null) {
            el.getOracleHierarchical().accept(this);
          }
        }

        @Override
        public void visit(Truncate el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.Truncate, "Truncate", recur);
          super.visit(el);
        }

        @Override
        public void visit(Update el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.Update, "Update", recur);
          visit(el.getTable());
          if (el.getStartJoins() != null) {
            for (Join join : el.getStartJoins()) {
              SimpleNode joinNode = join.getASTNode();
              RelationNode rn =
                  addRelationNode(join.toString(), NodeType.Join, "Join", joinNode, true);
              inClause = true;
              clauseNode = rn;
              join.getRightItem().accept(this);
              inClause = false;
            }
          }
          if (el.getExpressions() != null) {
            for (Expression expression : el.getExpressions()) {
              expression.accept(this);
            }
          }

          if (el.getFromItem() != null) {
            //            RelationNode rn =
            //                addRelationNode(el.getFromItem().toString(), NodeType.From, "From",
            // null, false);
            //            inClause = true;
            //            clauseNode = rn;
            el.getFromItem().accept(this);
            //            inClause = false;
          }

          if (el.getJoins() != null) {
            for (Join join : el.getJoins()) {
              SimpleNode joinNode = join.getASTNode();
              RelationNode rn =
                  addRelationNode(join.toString(), NodeType.Join, "Join", joinNode, true);
              inClause = true;
              clauseNode = rn;
              join.getRightItem().accept(this);
              inClause = false;
            }
          }

          if (el.getWhere() != null) {
            SimpleNode whereNode = el.getWhere().getASTNode();
            RelationNode rn =
                addRelationNode(el.getWhere().toString(), NodeType.Where, "Where", whereNode, true);
            inClause = true;
            clauseNode = rn;
            el.getWhere().accept(this);
            inClause = false;
          }
        }

        @Override
        public void visit(Merge el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.Merge, "Merge", recur);
          super.visit(el);
        }

        @Override
        public void visit(Alter el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          RelationNode rn = addRootNode(el, NodeType.Alter, "Alter", recur);
          el.getTable().accept(selectDeParser);
          el.getAlterExpressions()
              .forEach(alterExpression -> deparseAlterExpression(alterExpression, rn));
        }

        private void deparseAlterExpression(AlterExpression expr, Node parent) {
          if (expr.getColDataTypeList() != null && !expr.getColDataTypeList().isEmpty()) {
            expr.getColDataTypeList()
                .forEach(
                    columnDataType -> {
                      String name = columnDataType.getColumnName();
                      Type type = NodeType.Column;
                      for (String mark : mybatisParamMark) {
                        if (name.startsWith(mark)) {
                          name = name.substring(1, name.length() - 1);
                          type = NodeType.Parameter;
                          break;
                        }
                      }
                      ElementNode en =
                          new ElementNode(GraphUtil.nid(), Language.SQL, type, name, name, name);
                      graph.addVertex(en);
                      String idtf = "";
                      idtf =
                          idtf
                              + (URI.checkInvalidCh(((RelationNode) parent).getSymbol()))
                              + "/"
                              + (URI.checkInvalidCh(name));
                      URI uri = new URI(Protocol.ANY, Language.SQL, uriFilePath, idtf);
                      if (isInline) {
                        URI wrapUri = new URI(Protocol.ANY, wrapLang, uriFilePath, wrapIdentifier);
                        wrapUri.setInline(uri);
                        en.setUri(wrapUri);
                      } else {
                        en.setUri(uri);
                      }
                      GraphUtil.addURI(Language.SQL, en.getUri(), en);
                      graph.addEdge(parent, en, new Edge(GraphUtil.eid(), CHILD));
                    });
          } else if (expr.getColumnName() != null) {
            String colName = expr.getColumnName();
            Type type = NodeType.Column;
            for (String mark : mybatisParamMark) {
              if (colName.startsWith(mark)) {
                colName = colName.substring(1, colName.length() - 1);
                type = NodeType.Parameter;
                break;
              }
            }
            ElementNode en =
                new ElementNode(GraphUtil.nid(), Language.SQL, type, colName, colName, colName);
            graph.addVertex(en);
            String idtf = "";
            idtf =
                idtf
                    + (URI.checkInvalidCh(((RelationNode) parent).getSymbol()))
                    + "/"
                    + (URI.checkInvalidCh(colName));
            URI uri = new URI(Protocol.ANY, Language.SQL, uriFilePath, idtf);
            if (isInline) {
              URI wrapUri = new URI(Protocol.ANY, wrapLang, uriFilePath, wrapIdentifier);
              wrapUri.setInline(uri);
              en.setUri(wrapUri);
            } else {
              en.setUri(uri);
            }
            GraphUtil.addURI(Language.SQL, en.getUri(), en);
            graph.addEdge(parent, en, new Edge(GraphUtil.eid(), CHILD));
          }
        }

        @Override
        public void visit(WithItem el) {
          SimpleNode snode = (SimpleNode) el.getSubSelect().getASTNode().jjtGetParent();
          addRelationNode(el.toString(), NodeType.With, "With", snode, true);
          super.visit(el);
        }

        @Override
        public void visit(Function el) {
          SimpleNode snode = el.getASTNode();
          addRelationNode(el.toString(), NodeType.Function, "Function", snode, true);
          super.visit(el);
        }

        @Override
        public void visit(CaseExpression el) {
          SimpleNode snode = el.getASTNode();
          addRelationNode(el.toString(), NodeType.Switch, "Switch", snode, true);
          super.visit(el);
        }

        @Override
        public void visit(WhenClause el) {
          SimpleNode snode = el.getASTNode();
          addRelationNode(el.toString(), NodeType.When, "when", snode, true);
          super.visit(el);
        }

        @Override
        public void visitBinaryExpression(BinaryExpression el) {
          SimpleNode snode = el.getASTNode();
          boolean setEdge = !inClause;
          RelationNode rn =
              addRelationNode(
                  el.toString(), NodeType.Binary, el.getStringExpression(), snode, setEdge);
          if (inClause) {
            graph.addEdge(clauseNode, rn, new Edge(GraphUtil.eid(), CHILD));
            String idtf;
            idtf = identifierMap.get(clauseNode) + "/" + el.getStringExpression();
            identifierMap.put(rn, idtf);
          }
          super.visitBinaryExpression(el);
        }
      };

  private ExpressionDeParser expreDeParser =
      new ExpressionDeParser() {
        @Override
        public void visit(Column el) {
          SimpleNode snode = el.getASTNode();
          String name = el.getColumnName();
          String qn = el.getFullyQualifiedName();
          String snippet = el.toString();
          Type type = NodeType.Column;
          for (String mark : mybatisParamMark) {
            if (name.startsWith(mark)) {
              name = name.substring(1, name.length() - 1);
              qn = qn.substring(1, qn.length() - 1);
              snippet = snippet.substring(1, snippet.length() - 1);
              type = NodeType.Parameter;
              break;
            }
          }
          addElementNode(snippet, type, name, qn, snode);
          super.visit(el);
        }
      };

  private SelectDeParser selectDeParser =
      new SelectDeParser() {
        @Override
        public void visit(Table el) {
          SimpleNode snode = el.getASTNode();
          addElementNode(
              el.toString(), NodeType.Table, el.getName(), el.getFullyQualifiedName(), snode);
          super.visit(el);
        }
      };

  private StatementDeParser deParser =
      new StatementDeParser(expreDeParser, selectDeParser, new StringBuilder()) {
        @Override
        public void visit(Statements stmts) {
          stmts
              .getStatements()
              .forEach(
                  stmt -> {
                    clearForNextStatement();
                    stmt.accept(this);
                  });
        }

        @Override
        public void visit(CreateIndex el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.CreateIndex, "CreateIndex", recur);
          super.visit(el);
        }

        @Override
        public void visit(CreateTable el) {
          SimpleNode recur = (SimpleNode) el.getTable().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.CreateTable, "CreateTable", recur);
          super.visit(el);
        }

        @Override
        public void visit(CreateView el) {
          SimpleNode recur = (SimpleNode) el.getView().getASTNode().jjtGetParent();
          addRootNode(el, NodeType.CreateView, "CreateView", recur);
          super.visit(el);
          super.visit(el);
        }
      };

  private RelationNode addRootNode(Statement el, Type nodeType, String symbol, SimpleNode recur) {
    RelationNode rn =
        new RelationNode(GraphUtil.nid(), Language.SQL, nodeType, el.toString(), symbol);
    while (recur.jjtGetParent() != null) {
      recur = (SimpleNode) recur.jjtGetParent();
    }
    rn.setRange(getRange(recur));
    graph.addVertex(rn);
    rootNodePool.put(el, rn);

    identifierMap.put(rn, symbol);

    return rn;
  }

  private RelationNode addRelationNode(
      String snippet, Type nodeType, String symbol, SimpleNode snode, boolean setEdge) {
    RelationNode rn = new RelationNode(GraphUtil.nid(), Language.SQL, nodeType, snippet, symbol);
    graph.addVertex(rn);
    if (snode != null) {
      rn.setRange(getRange(snode));
      nodePool.put(snode, rn);
    }
    if (setEdge) findParentEdge(snode, rn);
    return rn;
  }

  private void addElementNode(
      String snippet, Type nodeType, String name, String qName, SimpleNode snode) {
    ElementNode en = new ElementNode(GraphUtil.nid(), Language.SQL, nodeType, snippet, name, qName);
    en.setRange(getRange(snode));
    graph.addVertex(en);
    nodePool.put(snode, en);
    findParentEdge(snode, en);

    URI uri = new URI(Protocol.USE, Language.SQL, uriFilePath, identifierMap.get(en));
    if (isInline) {
      URI wrapUri = new URI(Protocol.ANY, wrapLang, uriFilePath, wrapIdentifier);
      wrapUri.setInline(uri);
      en.setUri(wrapUri);
    } else {
      en.setUri(uri);
    }
    GraphUtil.addURI(Language.SQL, en.getUri(), en);
  }

  private void findParentEdge(SimpleNode snode, Node node) {
    StringBuilder idtf = new StringBuilder();

    if (node instanceof RelationNode) {
      idtf.append(URI.checkInvalidCh(((RelationNode) node).getSymbol()));
    } else if (node instanceof ElementNode) {
      idtf.append(URI.checkInvalidCh(((ElementNode) node).getName()));
    }

    if (snode == null) {
      for (Map.Entry<Statement, Node> entry : rootNodePool.entrySet()) {
        Node node1 = entry.getValue();
        graph.addEdge(node1, node, new Edge(GraphUtil.eid(), CHILD));
        idtf.insert(0, identifierMap.get(node1) + "/");
      }
      identifierMap.put(node, idtf.toString());
      return;
    }

    parentNode = (SimpleNode) snode.jjtGetParent();
    boolean found = false;
    while (parentNode != null) {
      Node pnode = nodePool.get(parentNode);
      if (pnode != null) {
        graph.addEdge(pnode, node, new Edge(GraphUtil.eid(), CHILD));
        idtf.insert(0, identifierMap.get(pnode) + "/");
        found = true;
        break;
      }
      parentNode = (SimpleNode) parentNode.jjtGetParent();
    }
    if (!found) {
      for (Map.Entry<Statement, Node> entry : rootNodePool.entrySet()) {
        Node node1 = entry.getValue();
        graph.addEdge(node1, node, new Edge(GraphUtil.eid(), CHILD));
        idtf.insert(0, identifierMap.get(node1) + "/");
      }
    }

    identifierMap.put(node, idtf.toString());
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
    identifierMap.clear();
    inClause = false;
    clauseNode = null;
  }

  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
    this.uriFilePath = FileUtil.getRelativePath(filePath);
  }
}
