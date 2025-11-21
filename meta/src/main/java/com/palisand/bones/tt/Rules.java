package com.palisand.bones.tt;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class Rules {
  @Builder.Default
  private EnabledChecker enabled = null;
  @Builder.Default
  private boolean notNull = false;

  public enum Severity {
    ERROR, WARNING
  }

  @FunctionalInterface
  public interface EnabledChecker {
    boolean check(Node<?> node) throws IOException;
  }

  @FunctionalInterface
  public interface NextGetter {
    Link<?, ?> next(Link<?, ?> t) throws IOException;
  }


  public record ConstraintViolation(Severity severity, Node<?> node, String field, String message,
      Exception exception) {
  }

  protected void doValidate(Validator validator, String field, Object value) throws IOException {
    if (notNull) {
      validator.assertNotNull(field, value);
    }
  }

  public boolean isEnabled(Node<?> node) throws IOException {
    if (enabled != null) {
      return enabled.check(node);
    }
    return true;
  }

  public static class RulesMap {
    private final Map<String, Rules> propertyRules = new TreeMap<>();

    public RulesMap and(String fieldName, Rules rules) {
      propertyRules.put(fieldName, rules);
      return this;
    }

    public Rules of(String fieldName, Function<String, Rules> superRules) {
      Rules rules = propertyRules.get(fieldName);
      if (rules != null) {
        return rules;
      }
      return superRules.apply(fieldName);
    }
  }

  public static RulesMap map() {
    return new RulesMap();
  }

  @SuperBuilder
  public static class BooleanRules extends Rules {

  }

  @Getter
  @Setter
  @SuperBuilder
  public static class NumberRules extends Rules {
    @Builder.Default
    private Double max = null;
    @Builder.Default
    private Double min = null;
    @Builder.Default
    private boolean notZero = false;
    @Builder.Default
    private int size = 40;
    @Builder.Default
    private int scale = 0;

    @Override
    protected void doValidate(Validator validator, String field, Object value) throws IOException {
      super.doValidate(validator, field, value);
      if (value != null) {
        Number number = (Number) value;
        if (max != null && number.doubleValue() > max) {
          validator.addViolation(field, "Value should not be higher than " + max);
        }
        if (min != null && number.doubleValue() < min) {
          validator.addViolation(field, "Value should not be lower than " + min);
        }
        if (notZero && number.longValue() == 0) {
          validator.addViolation(field, "Value should not be 0");
        }
        if (number.toString().length() > size + 1) {
          validator.addViolation(field, "Value should not have more digits than " + size);
        }
        if (new BigDecimal(number.toString()).scale() > scale) {
          validator.addViolation(field,
              "Value should not have more than " + scale + " digits after the decimal point");
        }
      }
    }
  }


  @Getter
  @Setter
  @SuperBuilder
  public static class StringRules extends Rules {
    @Builder.Default
    private int maxLength = Integer.MAX_VALUE;
    @Builder.Default
    private int minLength = Integer.MIN_VALUE;
    @Builder.Default
    private boolean notEmpty = false;
    @Builder.Default
    private String pattern = null;
    @Builder.Default
    private boolean multiLine = false;


    @Override
    protected void doValidate(Validator validator, String field, Object value) throws IOException {
      super.doValidate(validator, field, value);
      String str = (String) value;
      if (notEmpty && (str == null || str.isBlank())) {
        validator.addViolation(field, "Field " + field + " should contain text");
      }
      if (str != null) {
        if (str.length() < minLength) {
          validator.addViolation(field, "Value should be at least " + minLength + " long");
        }
        if (str.length() > maxLength) {
          validator.addViolation(field, "Value should be at most " + maxLength + " long");
        }
        if (pattern != null && !str.matches(pattern)) {
          validator.addViolation(field, "Value should match pattern: " + pattern);
        }
        if (!multiLine && str.contains("\n")) {
          validator.addViolation(field, "Value should not contain more than one line");
        }
      }
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  public static class EnumRules extends Rules {

    @Singular("notAllowed")
    private List<Object> notAllowed;

    @Override
    protected void doValidate(Validator validator, String field, Object value) throws IOException {
      super.doValidate(validator, field, value);
      if (value != null && notAllowed.contains(value)) {
        validator.addViolation(field, "Value " + value + " is not allowed for field " + field);
      }
    }

  }

  @Getter
  @Setter
  @SuperBuilder
  public static class ListRules extends Rules {
    @Builder.Default()
    private boolean notEmpty = false;

    @Override
    protected void doValidate(Validator validator, String field, Object value) throws IOException {
      super.doValidate(validator, field, value);
      List<?> list = (List<?>) value;
      if (notEmpty && list.isEmpty()) {
        validator.addViolation(field, "Field " + field + " should not be empty");
      }
    }

  }


  @Getter
  @Setter
  @SuperBuilder
  public static class LinkListRules extends Rules {
    @Builder.Default()
    private boolean notEmpty = false;

    @Override
    protected void doValidate(Validator validator, String field, Object value) throws IOException {
      super.doValidate(validator, field, value);
      LinkList<?, ?> list = (LinkList<?, ?>) value;
      if (notEmpty && list.isEmpty()) {
        validator.addViolation(field, "Field " + field + " should not be empty");
      }
    }

  }


  @Getter
  @Setter
  @SuperBuilder
  public static class LinkRules extends Rules {
    @Builder.Default()
    private NextGetter noCycle = null;

    @Override
    protected void doValidate(Validator validator, String field, Object value) throws IOException {
      super.doValidate(validator, field, value);
      Link<?, ?> link = (Link<?, ?>) value;
      if (isNotNull()) {
        validator.assertNotNull(field, link.get());
      }
      try {
        if (noCycle != null && !noCycle(link)) {
          validator.addViolation(field, "Field " + field + " creates an invalid cycle of links");
        }
      } catch (Exception ex) {
        validator.addViolation(field, ex);
      }
    }

    private boolean noCycle(Link<?, ?> link) throws IOException {
      if (link.get() != null) {
        try {
          Set<Link<?, ?>> found = new HashSet<>();
          found.add(link);
          Link<?, ?> check = noCycle.next(link);
          while (check != null && check.get() != null) {
            if (!found.add(check)) {
              return false;
            }
            check = noCycle.next(check);
          }
        } catch (Exception ex) {
          // ignore because the path to root is finished
        }
      }
      return true;
    }

  }


}
