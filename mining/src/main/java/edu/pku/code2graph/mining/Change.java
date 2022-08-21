package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URI;

public class Change {
  final String identifier;
  final String source;
  final URI uri;

  Change(String identifier, String source, URI uri) {
    this.identifier = identifier;
    this.source = source;
    this.uri = uri;
  }
}
