package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.model.URITree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class History extends ArrayList<URITree> {
  public void analyzeDiff(URITree head, URITree base, int decay) {
    Set<URI> additions = new HashSet<>();
    Set<URI> deletions = new HashSet<>();
    for (URI uri : head.keySet()) {
      if (!base.has(uri)) additions.add(uri);
    }
    for (URI uri : base.keySet()) {
      if (!head.has(uri)) deletions.add(uri);
    }
    return;
  }
}
