package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class EdgeType {
    // method declaration
    public static final Type METHOD_PARAMETER = type("METHOD_PARAMETER");
    public static final Type METHOD_RETURN = type("METHOD_RETURN");
    public static final Type METHOD_EXCEPTION = type("METHOD_EXCEPTION");

    // method invocation
    public static final Type METHOD_CALLER = type("METHOD_CALLER");
    public static final Type METHOD_CALLEE = type("METHOD_CALLEE");
    public static final Type METHOD_ARGUMENT = type("METHOD_ARGUMENT");

}
