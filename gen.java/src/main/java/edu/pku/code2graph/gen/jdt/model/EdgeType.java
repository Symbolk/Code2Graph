package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class EdgeType {
    // method declaration
    public static final Type METHOD_PARAMETER = type("parameter");
    public static final Type METHOD_RETURN = type("return");
    public static final Type METHOD_EXCEPTION = type("exception");

    // method invocation
    public static final Type METHOD_CALLER = type("caller");
    public static final Type METHOD_CALLEE = type("callee");
    public static final Type METHOD_ARGUMENT = type("argument");

}
