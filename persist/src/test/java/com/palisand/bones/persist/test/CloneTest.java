package com.palisand.bones.persist.test;

public class CloneTest {

  public String name;

  public Inner makeClone(Inner x) throws CloneNotSupportedException {
    return (Inner) x.clone();
  }

  public class Inner implements Cloneable {

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

    public void show() {
      System.out.println(CloneTest.this.name);
    }
  }

  public static void main(String... args) throws CloneNotSupportedException {
    CloneTest a = new CloneTest();
    a.name = "A";
    CloneTest b = new CloneTest();
    b.name = "B";
    Inner x = a.new Inner();
    b.makeClone(x).show();
  }

}
