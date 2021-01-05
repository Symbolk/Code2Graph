package edu.pku.code2graph.gen.jdt.model;

import edu.pku.code2graph.model.Type;

import static edu.pku.code2graph.model.TypeSet.type;

public class EdgeType {
    // type declaration
    public static final Type PARENT_CLASS = type("parent_class");
    public static final Type CHILD_CLASS = type("child_class");
    public static final Type IMPLEMENT_INTERFACE = type("implement_interface");

    // field and variable declaration
    public static final Type DATA_TYPE = type("data_type");

    // method declaration
    public static final Type METHOD_PARAMETER = type("parameter");
    public static final Type METHOD_RETURN = type("return_type");
    public static final Type METHOD_EXCEPTION = type("exception");

    // method invocation
    public static final Type METHOD_CALLER = type("caller");
    public static final Type METHOD_CALLEE = type("callee");
    public static final Type METHOD_ARGUMENT = type("argument");

    public static final Type BODY = type("body");
    public static final Type CHILD = type("child");
    public static final Type PARENT = type("parent");

}
