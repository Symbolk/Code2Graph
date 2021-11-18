package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.gen.sql.JsqlGenerator;
import edu.pku.code2graph.gen.xml.model.MybatisElement;
import edu.pku.code2graph.io.GraphVizExporter;
import edu.pku.code2graph.model.*;
import edu.pku.code2graph.util.FileUtil;
import edu.pku.code2graph.util.GraphUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.*;

import static edu.pku.code2graph.model.TypeSet.type;

public class MybatisMapperHandler extends AbstractHandler {
  private Map<String, Map<String, MybatisElement>> sqlMap = new HashMap<>();
  private Map<String, Map<String, MybatisElement>> queryMap = new HashMap<>();
  private Map<String, String> xmlToJavaMapper = new HashMap<>();
  private List<String> linkToken = Arrays.asList("where", "and", "or");

  private JsqlGenerator sqlGenerator = new JsqlGenerator();
  private String currentId;
  private MybatisElement currentEle;
  private ElementNode fileEle;
  private ElementNode mapperEle;
  private ElementNode queryEle;

  private Map<String, List<ElementNode>> identifierMap = sqlGenerator.getIdentifiers();
  private Map<String, List<RelationNode>> queryTypeMap = sqlGenerator.getQueries();

  @Override
  public void startDocument() throws SAXException {
    logger.debug("Start Parsing {}", uriFilePath);
    if (!sqlMap.containsKey(uriFilePath)) sqlMap.put(uriFilePath, new HashMap<>());
    if (!queryMap.containsKey(uriFilePath)) queryMap.put(uriFilePath, new HashMap<>());

    String name = FileUtil.getFileNameFromPath(filePath);
    URI uri = new URI(false, uriFilePath);
    uri.addLayer("", Language.XML);
    fileEle =
        new ElementNode(
            GraphUtil.nid(), Language.XML, type("file", true), "", name, uriFilePath, uri);
    graph.addVertex(fileEle);
    GraphUtil.addURI(Language.XML, fileEle.getUri(), fileEle);
    super.startDocument();
  }

  @Override
  public void endDocument() throws SAXException {
    logger.debug("\nEnd Parsing {}", uriFilePath);
    if (sqlMap.get(uriFilePath).isEmpty()) sqlMap.remove(uriFilePath);
    if (queryMap.get(uriFilePath).isEmpty()) queryMap.remove(uriFilePath);
    sqlGenerator.clearIdentifiers();
    sqlGenerator.clearQueries();
    fileEle = null;
    mapperEle = null;
    super.endDocument();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    String currentParam = null;
    String currentRes = null;
    switch (qName) {
      case "mapper":
        if (attributes != null) {
          String value = attributes.getValue("namespace");
          if (value != null) {
            xmlToJavaMapper.put(uriFilePath, value);
          }
        }
        URI mapperUri = new URI(false, uriFilePath);
        mapperUri.addLayer("mapper", Language.XML);
        mapperEle =
            new ElementNode(
                GraphUtil.nid(), Language.XML, type("mapper", true), "", qName, qName, mapperUri);

        graph.addVertex(mapperEle);
        graph.addEdge(fileEle, mapperEle, new Edge(GraphUtil.eid(), type("child")));
        GraphUtil.addURI(Language.XML, mapperUri, mapperEle);
        break;
      case "select":
        if (attributes != null) {
          String value = attributes.getValue("resultType");
          if (value != null) {
            currentRes = value;
          }
        }
      case "update":
      case "delete":
      case "insert":
        if (attributes != null) {
          String value = attributes.getValue("parameterType");
          if (value != null) {
            currentParam = value;
          }
        }
      case "sql":
        if (attributes != null) {
          String value = attributes.getValue("id");
          if (value != null) {
            currentId = value;
          }
        }

        URI queryUri = new URI(false, uriFilePath);
        queryUri.addLayer("mapper/" + qName, Language.XML);
        queryEle =
            new ElementNode(
                GraphUtil.nid(), Language.XML, type("query", true), "", qName, qName, queryUri);

        graph.addVertex(queryEle);
        GraphUtil.addURI(Language.XML, queryUri, queryEle);
        if (mapperEle != null)
          graph.addEdge(mapperEle, queryEle, new Edge(GraphUtil.eid(), type("child")));

        currentEle =
            new MybatisElement(
                currentId, currentParam, currentRes, "", qName.equals("sql"), queryEle);
        break;
      case "where":
        if (currentId != null && currentEle != null) {
          currentEle.setQuery(currentEle.getQuery() + "where");
        }
        break;
      case "include":
        String refid = null;
        if (attributes != null) {
          String value = attributes.getValue("refid");
          if (value != null) {
            refid = value;
          }
        }
        if (refid != null && currentId != null && currentEle != null) {
          MybatisElement sqlEle = sqlMap.get(uriFilePath).get(refid);
          if (sqlEle != null && sqlEle.getQuery() != null) {
            int curLine = currentEle.getQuery().split("\n").length;
            String[] sqlSplit = sqlEle.getQuery().split("\n");
            Range sqlRange =
                new Range(
                    curLine,
                    curLine + sqlSplit.length - 1,
                    0,
                    sqlSplit[sqlSplit.length - 1].length() - 1);
            currentEle.getIncludeRange().add(new ImmutablePair<>(refid, sqlRange));
            currentEle.setQuery(
                currentEle.getQuery()
                    + (currentEle.getQuery().isEmpty() ? "" : "\n")
                    + sqlEle.getQuery());
          }
        }
        break;
    }
    super.startElement(uri, localName, qName, attributes);
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    switch (qName) {
      case "sql":
        if (currentId != null && currentEle != null)
          sqlMap.get(uriFilePath).put(currentId, currentEle);
        currentId = null;
        currentEle = null;
        break;
      case "select":
      case "update":
      case "delete":
      case "insert":
        if (currentId != null && currentEle != null) {
          queryMap.get(uriFilePath).put(currentId, currentEle);
          String query = currentEle.getQuery();
          String idtf = "mapper/" + qName;
          Language lang = Language.XML;
          Graph<Node, Edge> graph = sqlGenerator.generate(query, filePath, lang, idtf, currentId);
          List<ElementNode> identifierById = identifierMap.get(currentId);
          List<RelationNode> queryById = queryTypeMap.get(currentId);
          List<ElementNode> identifierInQuery = new ArrayList<ElementNode>();
          int minLayerOfNode = 0;
          if (identifierById != null && !identifierById.isEmpty())
            minLayerOfNode = identifierById.get(0).getUri().getInline().getIdentifierSegmentCount();
          if (identifierById != null) {
            for (ElementNode node : identifierById) {
              String sqlId = ifInInclude(node);
              if (sqlId != null) {
                node.getUri().setIdentifier("mapper/sql");
                String lastToken = node.getUri().getSymbol();
                node.getUri().getInline().setIdentifier(lastToken);

                if (!graph.containsVertex(sqlMap.get(uriFilePath).get(sqlId).getNode())) {
                  logger.error("node not in graph");
                }

                graph.addEdge(
                    sqlMap.get(uriFilePath).get(sqlId).getNode(),
                    node,
                    new Edge(GraphUtil.eid(), type("child")));
              } else {
                identifierInQuery.add(node);
                minLayerOfNode =
                    Math.min(minLayerOfNode, node.getUri().getInline().getIdentifierSegmentCount());
              }

              currentEle.addIdentifer(node.getUri());
            }
          }

          if (queryById != null && !queryById.isEmpty()) {
            for (RelationNode rn : queryById) {
              graph.addEdge(queryEle, rn, new Edge(GraphUtil.eid(), type("child")));
            }
          } else if (identifierById != null) {
            for (ElementNode node : identifierInQuery) {
              if (node.getUri().getInline().getIdentifierSegmentCount() == minLayerOfNode) {
                graph.addEdge(queryEle, node, new Edge(GraphUtil.eid(), type("child")));
              }
            }
          }
        }
        queryEle = null;
        currentId = null;
        currentEle = null;
        break;
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (currentId != null && currentEle != null) {
      String content = new String(ch, start, length);
      //    System.out.print(tempString);
      if (content.trim().length() != 0) {
        String currentQuery = currentEle.getQuery();
        String[] queryTokens = currentQuery.split("\\s+");
        String lastToken = queryTokens[queryTokens.length - 1];
        String[] contentTokens = content.trim().split("\\s+");
        String firstToken = contentTokens[0];

        if (linkToken.contains(lastToken.toLowerCase())
            && linkToken.contains(firstToken.toLowerCase())) {
          content = content.replaceFirst(firstToken, "");
        }

        currentEle.setQuery(
            currentEle.getQuery() + (currentEle.getQuery().isEmpty() ? "" : "\n") + content.trim());
      }
    }
    super.characters(ch, start, length);
  }

  private String ifInInclude(ElementNode en) {
    Range elRange = en.getRange();
    List<Pair<String, Range>> includeRangeList = currentEle.getIncludeRange();

    int elStartLine = elRange.getStartLine(), elStartCol = elRange.getStartColumn();
    int elEndLine = elRange.getEndLine(), elEndCol = elRange.getEndColumn();

    for (Pair<String, Range> pair : includeRangeList) {
      Range range = pair.getRight();
      int inStartLine = range.getStartLine(), inStartCol = range.getStartColumn();
      int inEndLine = range.getEndLine(), inEndCol = range.getEndColumn();
      if ((elStartLine > inStartLine || elStartLine == inStartLine && elStartCol >= inStartCol)
          && (elEndLine < inEndLine || elEndLine == inEndLine && elEndCol <= inEndCol)) {
        return pair.getLeft();
      }
    }
    return null;
  }

  public Map<String, Map<String, MybatisElement>> getSqlMap() {
    return sqlMap;
  }

  public Map<String, Map<String, MybatisElement>> getQueryMap() {
    return queryMap;
  }

  public Map<String, String> getXmlToJavaMapper() {
    return xmlToJavaMapper;
  }
}
