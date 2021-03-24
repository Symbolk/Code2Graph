package com.github.test;

import java.io.IOException;

public class Test1 extends A implements B {
  private A a, b;

  {
    a = new A(5);
  }

  public A fun(A a) throws IOException {
    A b = new A();
    Integer x = a.getA();
    if (x <= 1) {
      return new A(1);
    } else {
      return new A(x * fun2(x - 1));
    }
  }

  public int fun2(int x) {
    return fun3(x);
  }

  private int fun3(int x) {
    return x * x;
  }
}

class A {
  private int a;

  public A(int a) {
    this.a = a;
    b.a accessor.attribute
            accessor.method
    caller  callee
  }

  public int getA() {
    return a;
  }
}

interface B {}
