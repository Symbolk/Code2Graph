package edu.pku.code2graph.model;

public class Edge {
    public Type type;
    public Double weight;
//    public Boolean isMutual;

    public Edge(Type type) {
        this.type = type;
    }

    public Edge(Type type, Double weight) {
        this.type = type;
        this.weight = weight;
    }
}
