package edu.pku.code2graph.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Commit {
  public final String hash;
  public final String parent;
  public final List<String> files = new ArrayList<>();

  public Commit() {
    hash = null;
    parent = null;
  }

  public Commit(String parent, String line) {
    this.parent = parent;
    StringTokenizer st = new StringTokenizer(line, ",");
    hash = st.nextToken();
    while (st.hasMoreTokens()) {
      files.add(st.nextToken());
    }
  }
}
