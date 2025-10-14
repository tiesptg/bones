package com.palisand.bones.persist.test;

import com.palisand.bones.persist.Database.Mapped;
import com.palisand.bones.persist.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public class V3 {

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class A extends Table {
    private String name;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class B extends A {
    private int theNumber;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @Mapped
  public static class M extends B {
    private String description;
  }


  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class C extends M {
    private double amount;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class D extends M {
    private boolean better;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class E extends A {
    public A a;
  }


}
