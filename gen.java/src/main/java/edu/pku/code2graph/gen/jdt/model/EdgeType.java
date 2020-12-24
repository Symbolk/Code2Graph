package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class EdgeType {
    private static final Type METHOD_CALLER = type("METHOD_CALLER");
    private static final Type METHOD_CALLEE = type("METHOD_CALLEE");
    private static final Type METHOD_ARGUMENT = type("METHOD_ARGUMENT");
    private static final Type METHOD_RETURN = type("METHOD_RETURN");

}
