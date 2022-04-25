package com.github.test;

import java.io.IOException;

@Param4
public class TestMember extends A implements B {
  @Param3
  private A prop1, prop2;

  // constructor
  public TestMember() {
    this(0);
  }

  // constructor
  public TestMember(int val) {
    super(val);
  }

  // initializer block
  {
    a = new A(5);
    // super field access
    super.a = new A(0);
  }

  // method decl
  @Param1("test")
  public A fun(@Param2("test2222") A a) throws IOException {
    // instance creation
    A b = new A();
    Integer x = a.getA();
    if (x <= 1) {
      return new AAA(1);
    } else {
      return new AAA(x * fun2(x - 1));
    }
  }

  public int fun2(int x) {
    // method invocation
    return aaa().bbb.fun3(x);
  }

  private int fun3(int x) {
    return x * x;
  }

  public int getA() {
    return a;
  }

  public int getParentA() {
    // super method invocation
    return super.getA();
  }
}

class A {
  protected int a;

  public A(int a) {
    this.a = a;
    //    b.a accessor.attribute
    //            accessor.method
    //    caller  callee
  }

  public int getA() {
    return a;
  }
}

interface B {}
