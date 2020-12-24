package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class EdgeType {
    public static final Type METHOD_CALLER = type("METHOD_CALLER");
    public static final Type METHOD_CALLEE = type("METHOD_CALLEE");
    public static final Type METHOD_ARGUMENT = type("METHOD_ARGUMENT");

    public static final Type METHOD_RETURN = type("METHOD_RETURN");

}
