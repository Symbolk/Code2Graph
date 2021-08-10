package edu.pku.code2graph.model;

public enum Language {
  C(".c"),
  CPP(".cpp"),
  HPP(".h"),
  JAVA(".java"),
  XML(".xml"),
  SQL(".sql"),
  HTML(".html");

  private String extension;

  Language(String extension) {
    this.extension = extension;
  }
}
