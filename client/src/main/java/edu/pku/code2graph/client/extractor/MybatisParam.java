package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.model.URI;

public class MybatisParam {
  String snippet;
  String symbol;
  URI uri;
  String className;
  Boolean isReturnType;
  public MybatisParam(Boolean ifReturn){
    isReturnType = ifReturn;
  }
  public MybatisParam(Boolean ifReturn, String symbol, URI uri, String className, String snippet){
    this.isReturnType = ifReturn;
    this.symbol = symbol;
    this.uri = uri;
    this.className = className;
    this.snippet = snippet;
  }
}
