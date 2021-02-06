package edu.pku.code2graph.diff.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Node;
import edu.pku.code2graph.model.RelationNode;
import edu.pku.code2graph.model.Type;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Mapping {
  private BiMap<Node, Node> one2one; // one to one mappings from a and b

  private Map<Type, Set<ElementNode>> unmatchedElementNodes1; // possibly deleted nodes
  private Map<Type, Set<RelationNode>> unmatchedRelationNodes1; // possibly added nodes

  private Map<Type, Set<ElementNode>> unmatchedElementNodes2; // possibly deleted nodes
  private Map<Type, Set<RelationNode>> unmatchedRelationNodes2; // possibly added nodes

  public Mapping() {
    this.one2one = HashBiMap.create();
    this.unmatchedElementNodes1 = new LinkedHashMap<>();
    this.unmatchedElementNodes2 = new LinkedHashMap<>();
    this.unmatchedRelationNodes1 = new LinkedHashMap<>();
    this.unmatchedRelationNodes2 = new LinkedHashMap<>();
  }

  public void addToMatched(Node a, Node b) {
    one2one.put(a, b);
  }

  public void addToUnmatched1(Node node) {
    if (node instanceof ElementNode) {
      if (!unmatchedElementNodes1.containsKey(node.getType())) {
        unmatchedElementNodes1.put(node.getType(), new HashSet<>());
      }
      unmatchedElementNodes1.get(node.getType()).add((ElementNode) node);
    } else {
      if (!unmatchedRelationNodes1.containsKey(node.getType())) {
        unmatchedRelationNodes1.put(node.getType(), new HashSet<>());
      }
      unmatchedRelationNodes1.get(node.getType()).add((RelationNode) node);
    }
  }

  public void addToUnmatched2(Node node) {
    if (node instanceof ElementNode) {
      if (!unmatchedElementNodes2.containsKey(node.getType())) {
        unmatchedElementNodes2.put(node.getType(), new HashSet<>());
      }
      unmatchedElementNodes2.get(node.getType()).add((ElementNode) node);
    } else {
      if (!unmatchedRelationNodes2.containsKey(node.getType())) {
        unmatchedRelationNodes2.put(node.getType(), new HashSet<>());
      }
      unmatchedRelationNodes2.get(node.getType()).add((RelationNode) node);
    }
  }

  public void removeFromUnmatched1(Node node) {
    if (node instanceof ElementNode) {
      if (unmatchedElementNodes1.containsKey(node.getType())) {
        unmatchedElementNodes1.get(node.getType()).remove(node);
        if (unmatchedElementNodes1.get(node.getType()).isEmpty()) {
          unmatchedElementNodes1.remove(node.getType());
        }
      }
    } else {
      if (unmatchedRelationNodes1.containsKey(node.getType())) {
        unmatchedRelationNodes1.get(node.getType()).remove(node);
        if (unmatchedRelationNodes1.get(node.getType()).isEmpty()) {
          unmatchedRelationNodes1.remove(node.getType());
        }
      }
    }
  }

  public void removeFromUnmatched2(Node node) {
    if (node instanceof ElementNode) {
      if (unmatchedElementNodes2.containsKey(node.getType())) {
        unmatchedElementNodes2.get(node.getType()).remove(node);
        if (unmatchedElementNodes2.get(node.getType()).isEmpty()) {
          unmatchedElementNodes2.remove(node.getType());
        }
      }
    } else {
      if (unmatchedRelationNodes2.containsKey(node.getType())) {
        unmatchedRelationNodes2.get(node.getType()).remove(node);
        if (unmatchedRelationNodes2.get(node.getType()).isEmpty()) {
          unmatchedRelationNodes2.remove(node.getType());
        }
      }
    }
  }

  public BiMap<Node, Node> getOne2one() {
    return one2one;
  }

  public Map<Type, Set<ElementNode>> getUnmatchedElementNodes1() {
    return unmatchedElementNodes1;
  }

  public Map<Type, Set<RelationNode>> getUnmatchedRelationNodes1() {
    return unmatchedRelationNodes1;
  }

  public Map<Type, Set<ElementNode>> getUnmatchedElementNodes2() {
    return unmatchedElementNodes2;
  }

  public Map<Type, Set<RelationNode>> getUnmatchedRelationNodes2() {
    return unmatchedRelationNodes2;
  }

  public Set<Node> getAllUnmatchedNodes(Version version) {
    Set<Node> diffNodes = new HashSet<>();
    if (version.equals(Version.A)) {
      unmatchedElementNodes1.forEach((key, value) -> diffNodes.addAll(value));
      unmatchedRelationNodes1.forEach((key, value) -> diffNodes.addAll(value));
    } else {
      unmatchedElementNodes2.forEach((key, value) -> diffNodes.addAll(value));
      unmatchedRelationNodes2.forEach((key, value) -> diffNodes.addAll(value));
    }
    return diffNodes;
  }
}
