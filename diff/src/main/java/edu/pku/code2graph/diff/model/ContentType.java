package edu.pku.code2graph.diff.model;

/** Type of the content in hunk */
public enum ContentType {
  COMMENT("Comment"), // pure comment
  CODE("Code"), // actual code (or mixed)
  BLANKLINE("BlankLine"), // blank lines
  EMPTY("Empty"), // added/deleted
  BINARY("Binary"); // binary content

  public String label;

  ContentType(String label) {
    this.label = label;
  }
}
