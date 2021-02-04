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
  public BiMap<Node, Node> one2one; // exactly matched signatures, used to match unmatched nodes

  public Map<Type, Set<ElementNode>> unmatchedElementNodes1; // possibly deleted nodes
  public Map<Type, Set<RelationNode>> unmatchedRelationNodes1; // possibly added nodes

  public Map<Type, Set<ElementNode>> unmatchedElementNodes2; // possibly deleted nodes
  public Map<Type, Set<RelationNode>> unmatchedRelationNodes2; // possibly added nodes

  public Mapping() {
    this.one2one = HashBiMap.create();
    this.unmatchedElementNodes1 = new LinkedHashMap<>();
    this.unmatchedElementNodes2 = new LinkedHashMap<>();
    this.unmatchedRelationNodes1 = new LinkedHashMap<>();
    this.unmatchedRelationNodes2 = new LinkedHashMap<>();
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
}
