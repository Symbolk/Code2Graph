package edu.pku.code2graph.gen.xml;

import edu.pku.code2graph.gen.sql.JsqlGenerator;
import edu.pku.code2graph.gen.xml.model.MybatisElement;
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
  private List<String> linkToken = Arrays.asList("where", "set", "and", "or");
  private Stack<String> suffixToken = new Stack<>();
  private Stack<Boolean> shoulTrim = new Stack<>();

  private JsqlGenerator sqlGenerator = new JsqlGenerator();
  private String currentId;
  private MybatisElement currentEle;
  private ElementNode fileEle;
  private ElementNode mapperEle;
  private ElementNode queryEle;

  @Override
  public void startDocument() throws SAXException {
    logger.debug("Start Parsing {}", uriFilePath);
    if (!sqlMap.containsKey(uriFilePath)) sqlMap.put(uriFilePath, new HashMap<>());
    if (!queryMap.containsKey(uriFilePath)) queryMap.put(uriFilePath, new HashMap<>());

    String name = FileUtil.getFileNameFromPath(filePath);
    URI uri = new URI(false, uriFilePath);
    fileEle =
        new ElementNode(
            GraphUtil.nid(), Language.XML, type("file", true), "", name, uriFilePath, uri);
    GraphUtil.addNode(fileEle);
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
    String prefix = "";
    String suffix = "";
    boolean oveerideSuffix = false;
    if (attributes != null) {
      prefix = attributes.getValue("prefix");
      suffix = attributes.getValue("suffix");
      oveerideSuffix = attributes.getValue("suffixOverrides") != null;
      prefix = prefix == null ? "" : prefix;
      suffix = suffix == null ? "" : suffix;
      suffixToken.push(suffix);
      shoulTrim.push(oveerideSuffix);
    }
    if (prefix.length() > 0)
      currentEle.setQuery(
          currentEle.getQuery() + (currentEle.getQuery().length() == 0 ? "" : "\n") + prefix);
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

        GraphUtil.addNode(mapperEle);
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
        if (attributes != null) {
          int attributesLen = attributes.getLength();
          for (int i = 0; i < attributesLen; i++) {
            String attrName = attributes.getQName(i);
            if (attrName.equals("id")) attrName = "queryId";
            String attrVal = attributes.getValue(i);
            queryUri.getLayer(1).put(attrName, attrVal);
          }
        }
        queryEle =
            new ElementNode(
                GraphUtil.nid(), Language.XML, type("query", true), "", qName, qName, queryUri);

        GraphUtil.addNode(queryEle);

        currentEle =
            new MybatisElement(
                currentId, currentParam, currentRes, "", qName.equals("sql"), queryEle);
        break;
      case "where":
        if (currentId != null && currentEle != null) {
          currentEle.setQuery(currentEle.getQuery() + "\nwhere");
        }
        break;
      case "set":
        if (currentId != null && currentEle != null) {
          currentEle.setQuery(currentEle.getQuery() + "\nset");
        }
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

    for (int i = 0; i < attributes.getLength(); i++) {
      String attrName = attributes.getQName(i);
      String attrVal = attributes.getValue(i);
      URI attrUri = new URI(false, uriFilePath);
      attrUri.addLayer(
          qName.equals("mapper") ? qName + "/" + attrName : "mapper/" + qName + "/" + attrName,
          Language.XML);
      attrUri.addLayer(URI.checkInvalidCh(attrVal), Language.ANY);

      ElementNode attrEle =
          new ElementNode(
              GraphUtil.nid(),
              Language.ANY,
              type("attribute", true),
              "",
              attrVal,
              attrVal,
              attrUri);

      GraphUtil.addNode(attrEle);
    }
    super.startElement(uri, localName, qName, attributes);
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    String suffix = suffixToken.pop();
    boolean toTrim = shoulTrim.pop();
    if (toTrim) currentEle.setQuery(trimQuery(currentEle.getQuery()));
    if (suffix.length() > 0) currentEle.setQuery(currentEle.getQuery() + " " + suffix);
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
          query = trimQuery(query);
          String idtf = "mapper/" + qName;
          Language lang = Language.XML;
          Graph<Node, Edge> graph =
              sqlGenerator.generate(
                  query, filePath, lang, idtf, currentEle.getNode().getUri(), currentId);
          List<ElementNode> identifierById = sqlGenerator.getIdentifiers().get(currentId);
          List<RelationNode> queryById = sqlGenerator.getQueries().get(currentId);
          List<ElementNode> identifierInQuery = new ArrayList<>();
          int minLayerOfNode = 0;
          if (identifierById != null && !identifierById.isEmpty())
            minLayerOfNode =
                getIdentifierSegmentCount(identifierById.get(0).getUri().getInlineIdentifier());
          if (identifierById != null) {
            // TODO: check concurrency
            for (ElementNode node : identifierById) {
              String sqlId = ifInInclude(node);
              if (sqlId != null) {
                node.getUri().setIdentifier("mapper/sql");
                String lastToken = node.getUri().getSymbol();
                node.getUri().setInlineIdentifier(lastToken);
              } else {
                identifierInQuery.add(node);
                minLayerOfNode =
                    Math.min(
                        minLayerOfNode,
                        getIdentifierSegmentCount(node.getUri().getInlineIdentifier()));
              }

              currentEle.addIdentifer(node.getUri());
              GraphUtil.addNode(node);
            }
          }
        }
        queryEle = null;
        currentId = null;
        currentEle = null;
        break;
    }
  }

  private String trimQuery(String query) {
    while (query.endsWith(",")) {
      query = query.substring(0, query.length() - 1);
    }
    String[] queryTokens = query.split("\\s+");
    String lastToken = queryTokens[queryTokens.length - 1];
    if (linkToken.contains(lastToken.toLowerCase())) {
      query = replaceLast(query, lastToken, "");
    }
    return query;
  }

  public static String replaceLast(String text, String strToReplace, String replaceWithThis) {
    int split = text.lastIndexOf(strToReplace);
    return text.substring(0, split)
        + replaceWithThis
        + text.substring(split + strToReplace.length());
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (currentId != null && currentEle != null) {
      String content = new String(ch, start, length);
      //    System.out.print(tempString);
      if (content.trim().length() != 0) {
        String currentQuery = currentEle.getQuery();
        String[] queryTokens = currentQuery.trim().split("\\s+");
        String lastToken = queryTokens[queryTokens.length - 1];
        String[] contentTokens = content.trim().split("\\s+");
        String firstToken = contentTokens[0];

        if ((linkToken.contains(lastToken.toLowerCase()) || lastToken.endsWith("("))
            && linkToken.contains(firstToken.toLowerCase())) {
          content = content.replaceFirst(firstToken, "");
        } else if (linkToken.contains(firstToken.toLowerCase()) && lastToken.endsWith(",")) {
          String oldLastToken = lastToken;
          while (lastToken.endsWith(","))
            lastToken = lastToken.substring(0, lastToken.length() - 1);
          currentQuery = replaceLast(currentQuery, oldLastToken, lastToken);
          currentEle.setQuery(currentQuery);
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

  private int getIdentifierSegmentCount(String identifier) {
    return identifier.replaceAll("\\\\/", "").split("/").length;
  }
}
