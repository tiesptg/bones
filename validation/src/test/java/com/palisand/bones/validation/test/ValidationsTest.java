package com.palisand.bones.validation.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import com.palisand.bones.validation.CamelCase;
import com.palisand.bones.validation.KebabCase;
import com.palisand.bones.validation.LowerCase;
import com.palisand.bones.validation.Max;
import com.palisand.bones.validation.NoXss;
import com.palisand.bones.validation.NotEmpty;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.Rules.PredicateWithException;
import com.palisand.bones.validation.Rules.Violation;
import com.palisand.bones.validation.SnakeCase;
import com.palisand.bones.validation.UpperCase;
import com.palisand.bones.validation.ValidWhen;
import com.palisand.bones.validation.Validator;
import lombok.Data;

class ValidationsTest {

  @Data
  public static class X {

    public static class XTest implements PredicateWithException<X> {
      @Override
      public boolean test(ValidationsTest.X x) {
        return x.field <= 5;
      }
    }

    @Max(10) private int field = 5;
    @ValidWhen(XTest.class)
    @NotNull private X x = null;
  }

  @Test
  void testValidator() {
    Validator validator = new Validator();
    X x = new X();
    X child = new X();
    child.setField(12);
    x.setX(child);
    List<Violation> result = validator.validate(x);
    System.out.println(result);
    assertTrue(result.size() == 1);
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Trial {
    Class<? extends Predicate<?>> test();
  }

  public static class TestVictim implements Predicate<Victim> {

    @Override
    public boolean test(ValidationsTest.Victim victim) {
      return victim != null;
    }
  }

  @Trial(test = TestVictim.class)
  public static class Victim {

  }

  @Test
  void testMisc() throws Exception {
    Validator validator = new Validator();
    X x = new X();
    x.setField(8);
    List<Violation> violations = validator.validate(x);
    assertTrue(violations.isEmpty());
    x.setField(0);
    violations = validator.validate(x);
    assertTrue(violations.size() == 1);
    x.x = new X();
    x.x.setField(8);
    violations = validator.validate(x);
    assertTrue(violations.isEmpty());
  }

  @Data
  public static class Fields {
    public Fields() {
      list.add(new Object());
    }

    @NoXss private String html = "";
    @CamelCase(startsWithCapitel = false) private String javaName;
    @NotNull private Object nullable = new Object();
    @CamelCase private String className;
    @NotEmpty private List<Object> list = new ArrayList<>();
    @SnakeCase private String jsonName;
    @KebabCase private String cobolName;
    @UpperCase private String upperName;
    @LowerCase private String lowerName;
  }

  @Test
  void testRules() {
    Validator validator = new Validator();
    Fields fields = new Fields();
    assertTrue(validator.validate(fields).isEmpty());
    fields.setHtml("hello <script>alert('Hello');</script>M&T");
    assertTrue(validator.validate(fields).size() == 1);
    System.out.println(fields.getHtml());
    assertEquals("hello &lt;script&gt;alert('Hello');&lt;/script&gt;M&T", fields.getHtml());
    assertTrue(validator.validate(fields).isEmpty());
    assertEquals("hello &lt;script&gt;alert('Hello');&lt;/script&gt;M&T", fields.getHtml());
    fields.setJavaName("Hello  There i will See-you_later");
    assertTrue(validator.validate(fields).size() == 1);
    System.out.println(fields.getJavaName());
    assertEquals("helloThereIWillSeeYouLater", fields.getJavaName());
    fields.setClassName("Hello  There i will See-you_later");
    assertTrue(validator.validate(fields).size() == 1);
    System.out.println(fields.getClassName());
    assertEquals("HelloThereIWillSeeYouLater", fields.getClassName());
    fields.setJsonName("Hello  There i will See-you_later");
    assertTrue(validator.validate(fields).size() == 1);
    System.out.println(fields.getJsonName());
    assertEquals("hello_there_i_will_see_you_later", fields.getJsonName());
    fields.setCobolName("Hello  There i will See-you_later");
    assertTrue(validator.validate(fields).size() == 1);
    System.out.println(fields.getCobolName());
    assertEquals("HELLO-THERE-I-WILL-SEE-YOU-LATER", fields.getCobolName());
    fields.setCobolName("HelloThereIWillSeeYouLater");
    assertTrue(validator.validate(fields).size() == 1);
    System.out.println(fields.getCobolName());
    assertEquals("HELLO-THERE-I-WILL-SEE-YOU-LATER", fields.getCobolName());
    fields.setJavaName("hello_there_i_will_see_you_later");
    assertEquals(1, validator.validate(fields).size());
    System.out.println(fields.getJavaName());
    assertEquals("helloThereIWillSeeYouLater", fields.getJavaName());
    fields.setUpperName("This is a W0nderFull Id#@");
    assertTrue(validator.validate(fields).size() == 1);
    System.out.println(fields.getUpperName());
    assertEquals("THIS IS A W0NDERFULL ID#@", fields.getUpperName());
    fields.setLowerName("This is a W0nderFull Id#@");
    assertTrue(validator.validate(fields).size() == 1);
    System.out.println(fields.getLowerName());
    assertEquals("this is a w0nderfull id#@", fields.getLowerName());
    fields.setNullable(null);
    assertEquals(1, validator.validate(fields).size());
    fields.setNullable(new Object());
    assertTrue(validator.validate(fields).isEmpty());
    fields.getList().clear();
    assertEquals(1, validator.validate(fields).size());
    fields.getList().add(new Object());
    assertTrue(validator.validate(fields).isEmpty());
  }

}
