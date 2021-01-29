package edu.pku.code2graph.diff.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Mapping {
  public BiMap<Node, Node> one2one; // exactly matched signatures, used to match unmatched nodes
  public Map<Type, List<Node>> unmatched1; // possibly deleted nodes
  public Map<Type, List<Node>> unmatched2; // possibly added nodes

  public Mapping() {
    this.one2one = HashBiMap.create();
    this.unmatched1 = new LinkedHashMap<>();
    this.unmatched2 = new LinkedHashMap<>();
  }

  public void addUnmatched1(Node node) {
    if (!unmatched1.containsKey(node.getType())) {
      unmatched1.put(node.getType(), new ArrayList<>());
    }
    unmatched1.get(node.getType()).add(node);
  }

  public void addUnmatched2(Node node) {
    if (!unmatched2.containsKey(node.getType())) {
      unmatched2.put(node.getType(), new ArrayList<>());
    }
    unmatched2.get(node.getType()).add(node);
  }
}
