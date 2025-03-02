package com.palisand.bones.tt;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class TypedTextErrorsTest {

  @Test
  void emptyTest() {
    Repository repository = new Repository();
    Exception ex = assertThrows(IOException.class, () -> {
      repository.read("src/test/resources/empty.tt");
    });
    System.out.println(ex);
  }

  @Test
  void typeTest() {
    Repository repository = new Repository();
    IOException ex = assertThrows(IOException.class, () -> {
      repository.read("src/test/resources/notype.tt");
    });
    System.out.println(ex);
  }

  @Test
  void wrongTypeTest() {
    Repository repository = new Repository();
    IOException ex = assertThrows(IOException.class, () -> {
      repository.read("src/test/resources/wrongtype.tt");
    });
    System.out.println(ex);
  }

  @Test
  void wrongMarginTest() {
    Repository repository = new Repository();
    IOException ex = assertThrows(IOException.class, () -> {
      repository.read("src/test/resources/wrongmargin.tt");
    });
    System.out.println(ex);
  }

}
