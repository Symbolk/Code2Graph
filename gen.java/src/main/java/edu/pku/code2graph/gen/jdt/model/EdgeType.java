package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class EdgeType {
    // type hierarchy
    public static final Type PARENT_CLASS = type("parent_class");
    public static final Type CHILD_CLASS = type("child_class");
    public static final Type IMPLEMENTATION = type("implementation");

    // type and source
    public static final Type DATA_TYPE = type("data_type");
    public static final Type REFERENCE = type("reference");

    // roles for method declaration
    public static final Type METHOD_PARAMETER = type("parameter");
    public static final Type METHOD_RETURN = type("return_type");
    public static final Type METHOD_EXCEPTION = type("exception");

    // roles for method invocation
    public static final Type METHOD_CALLER = type("caller");
    public static final Type METHOD_CALLEE = type("callee");
    public static final Type METHOD_ARGUMENT = type("argument");

    // nesting
    public static final Type BODY = type("body");
    public static final Type CHILD = type("child");

    // operand
    public static final Type LEFT = type("left");
    public static final Type RIGHT = type("right");

}