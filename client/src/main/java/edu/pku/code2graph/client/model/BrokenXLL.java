package edu.pku.code2graph.client.model;

import edu.pku.code2graph.model.Range;
import edu.pku.code2graph.model.URI;

public class BrokenXLL {
  Range range;
  String rules;
  URI uri;
  String programmingLanguage;

  public BrokenXLL(Range range, String rules, URI uri, String programmingLanguage){
    this.range = range;
    this.rules = rules;
    this.uri = uri;
    this.programmingLanguage = programmingLanguage;
  }
}
