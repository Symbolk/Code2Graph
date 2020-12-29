package edu.pku.code2graph.model;

public class Edge {
  private Integer id;
  public Type type;
  public Double weight;
  //    public Boolean isMutual;

  public Edge(Integer id, Type type) {
    this.id = id;
    this.type = type;
  }

  public Edge(Integer id, Type type, Double weight) {
    this.id = id;
    this.type = type;
    this.weight = weight;
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
}
