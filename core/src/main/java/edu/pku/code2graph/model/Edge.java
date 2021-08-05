package edu.pku.code2graph.model;

import java.io.Serializable;

public class Edge implements Serializable {
  private static final long serialVersionUID = -8788348413604586284L;

  private final Integer id;
  private final Type type;
  private Double weight;
  // directed (default) or bidirectional/mutual
  public Boolean isMutual = false;
  // intra-lang (default) or cross-lang (XLL)
  public Boolean isXLL = false;
  // hierarchical (default) or semantic
  public Boolean isSemantic = false;

  public Edge(Integer id, Type type) {
    this.id = id;
    this.type = type;
  }

  public Edge(Integer id, Type type, Double weight) {
    this.id = id;
    this.type = type;
    this.weight = weight;
  }

  public Edge(Integer id, Type type, Double weight, Boolean isMutual, Boolean isXLL) {
    this.id = id;
    this.type = type;
    this.weight = weight;
    this.isMutual = isMutual;
    this.isXLL = isXLL;
  }

  public Integer getId() {
    return id;
  }

  public Type getType() {
    return type;
  }

  public Double getWeight() {
    return weight;
  }

  public void setWeight(Double weight) {
    this.weight = weight;
  }
}
