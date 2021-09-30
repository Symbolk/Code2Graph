package edu.pku.code2graph.gen.xml.model;

import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Range;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class MybatisElement {
  String id;
  String parameterType;
  String resultType;
  String query;
  Boolean isSqlTag;
  List<Pair<String, Range>> includeRange = new ArrayList<>();
  ElementNode node;
  List<URI> identifierList = new ArrayList<>();

  public MybatisElement(
      String id,
      String parameterType,
      String resultType,
      String query,
      Boolean isSqlTag,
      ElementNode node) {
    this.id = id;
    this.parameterType = parameterType;
    this.resultType = resultType;
    this.query = query;
    this.isSqlTag = isSqlTag;
    this.node = node;
  }

  public void addIdentifer(URI uri) {
    identifierList.add(uri);
  }

  public String getId() {
    return id;
  }

  public String getParameterType() {
    return parameterType;
  }

  public void setParameterType(String parameterType) {
    this.parameterType = parameterType;
  }

  public String getResultType() {
    return resultType;
  }

  public void setResultType(String resultType) {
    this.resultType = resultType;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public Boolean getSqlTag() {
    return isSqlTag;
  }

  public void setSqlTag(Boolean sqlTag) {
    isSqlTag = sqlTag;
  }

  public List<Pair<String, Range>> getIncludeRange() {
    return includeRange;
  }

  public ElementNode getNode() {
    return node;
  }

  public List<URI> getIdentifierList() {
    return identifierList;
  }
}
