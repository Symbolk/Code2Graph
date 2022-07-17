package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.URIBase;
import edu.pku.code2graph.xll.LinkBase;

public class Candidate extends LinkBase {
  public int confidence;

  public Candidate(URIBase def, URIBase use, String name) {
    super(def, use, name);
  }
}
