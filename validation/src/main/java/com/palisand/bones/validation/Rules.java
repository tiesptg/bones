package com.palisand.bones.validation;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.palisand.bones.Classes.Property;
import com.palisand.bones.Names;

public class Rules {
  private final static Map<Class<?>, Rule> RULES = new HashMap<>();

  public static Rule getRule(Class<?> annotationClass) {
    return RULES.get(annotationClass);
  }

  @FunctionalInterface
  public interface PredicateWithException<A> {
    boolean test(A a) throws Exception;
  }

  public static <A extends Annotation> void addRule(Class<A> annotationClass, Rule rule) {
    RULES.put(annotationClass, rule);
  }

  public enum Severity {
    ERROR, WARNING
  }

  public record Violation(Severity severity, Object ownerOfField, Property<?> property,
      String message, Exception exception) {

  }

  @FunctionalInterface
  public interface Rule {
    Object validate(List<Violation> violations, Object ownerOfField, Object spec,
        Property<?> property, Object value) throws Exception;
  }

  private static void registerNotNull() {
    addRule(NotNull.class, (violations, ownerOfField, spec, property, value) -> {
      if (value == null || (value instanceof String str && str.isBlank())) {
        violations.add(new Violation(Severity.ERROR, ownerOfField, property,
            "value should not be null", null));
      }
      return value;
    });
  }

  private static void registerNotEmpty() {
    addRule(NotEmpty.class, (violations, ownerOfField, spec, property, value) -> {
      if (value == null) {
        violations.add(new Violation(Severity.ERROR, ownerOfField, property,
            "value should not be null", null));
      }
      if (value instanceof Collection collection) {
        if (collection.isEmpty()) {
          violations.add(new Violation(Severity.ERROR, ownerOfField, property,
              "value should not be empty", null));
        }
      }
      return value;
    });
  }

  private static void registerLength() {
    addRule(Length.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        Length length = (Length) spec;
        if (value.toString().length() < length.min()) {
          violations.add(new Violation(Severity.ERROR, ownerOfField, property,
              "value should be " + length.min() + " or longer", null));
        }
        if (length.max() != -1 && value.toString().length() > length.max()) {
          violations.add(new Violation(Severity.ERROR, ownerOfField, property,
              "value should be " + length.max() + " of shorter", null));
        }
      }
      return value;
    });
  }

  private static void registerNoXss() {
    addRule(NoXss.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null && value.toString().matches(".*[\\<\\>].*")) {
        StringBuilder sb = new StringBuilder(value.toString());
        for (int i = 0; i < sb.length(); ++i) {
          switch (sb.charAt(i)) {
            case '<':
              sb.replace(i, ++i, "&lt;");
              i += 2;
              break;
            case '>':
              sb.replace(i, ++i, "&gt;");
              i += 2;
              break;
          }
        }
        violations.add(new Violation(Severity.WARNING, ownerOfField, property,
            "value constains < or > characters. This may indicate a XSS attack", null));
        return sb.toString();
      }
      return value;

    });
  }

  private static void registerRegexpPattern() {
    addRule(RegexPattern.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        RegexPattern pattern = (RegexPattern) spec;
        if (!value.toString().matches(pattern.value())) {
          violations.add(new Violation(Severity.ERROR, ownerOfField, property,
              "value does not conform to pattern :'" + pattern.value() + "'", null));
        }
      }
      return value;
    });
  }

  private static void registerMax() {
    addRule(Max.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        Max max = (Max) spec;
        Number number = (Number) value;
        if ((max.inclusive() && number.doubleValue() > max.value())
            || (!max.inclusive() && number.doubleValue() >= max.value())) {
          violations.add(
              new Violation(Severity.ERROR, ownerOfField, property, "number is too large", null));
        }
      }
      return value;
    });
  }

  private static void registerMin() {
    addRule(Min.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        Min min = (Min) spec;
        Number number = (Number) value;
        if ((min.inclusive() && number.doubleValue() < min.value())
            || (!min.inclusive() && number.doubleValue() <= min.value())) {
          violations.add(
              new Violation(Severity.ERROR, ownerOfField, property, "number is too small", null));
        }
      }
      return value;
    });
  }

  private static void registerBefore() {
    addRule(Before.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        Before before = (Before) spec;
        Instant instant = parseIsoInstant(before.value());
        Instant timestamp = convertToInstant(value);
        if (timestamp.isAfter(instant)) {
          violations.add(new Violation(Severity.ERROR, ownerOfField, property,
              "value should be before " + before.value(), null));
        }
      }
      return value;
    });
  }

  private static void registerAfter() {
    addRule(After.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        After after = (After) spec;
        Instant instant = parseIsoInstant(after.value());
        Instant timestamp = convertToInstant(value);
        if (timestamp.isBefore(instant)) {
          violations.add(new Violation(Severity.ERROR, ownerOfField, property,
              "value should be before " + after.value(), null));
        }
      }
      return value;
    });
  }

  private static void registerUpperCase() {
    addRule(UpperCase.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null && value.toString().matches(".*[a-z].*")) {
        violations.add(new Violation(Severity.WARNING, ownerOfField, property,
            "value contains non uppercase characters", null));
        return value.toString().toUpperCase();
      }
      return value;
    });
  }

  private static void registerLowerCase() {
    addRule(LowerCase.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null && value.toString().matches(".*[A-Z].*")) {
        violations.add(new Violation(Severity.WARNING, ownerOfField, property,
            "value contains non lowercase characters", null));
        return value.toString().toLowerCase();
      }
      return value;
    });
  }

  private static void registerCamelCase() {
    addRule(CamelCase.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null && value.toString().matches(".*[\\-_\\s].*")) {
        CamelCase cc = (CamelCase) spec;
        String result = Names.toCamelCase(value.toString(), cc.startsWithCapitel());
        if (!result.equals(value)) {
          violations.add(new Violation(Severity.WARNING, ownerOfField, property,
              "value was not camel case", null));
          return result;
        }
      }
      return value;
    });
  }

  private static void registerSnakeCase() {
    addRule(SnakeCase.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        SnakeCase sc = (SnakeCase) spec;
        String result = Names.toSeparatedCase(value.toString(), sc.upperCase(), '_');
        if (!result.equals(value)) {
          violations.add(new Violation(Severity.WARNING, ownerOfField, property,
              "value was not snake case", null));
          return result;
        }
      }
      return value;
    });
  }

  private static void registerKebabCase() {
    addRule(KebabCase.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        KebabCase sc = (KebabCase) spec;
        String result = Names.toSeparatedCase(value.toString(), sc.upperCase(), '-');
        if (!result.equals(value)) {
          violations.add(new Violation(Severity.WARNING, ownerOfField, property,
              "value was not kebab case", null));
          return result;
        }
      }
      return value;
    });
  }

  private static void registerNotAllowed() {
    addRule(NotAllowed.class, (violations, ownerOfField, spec, property, value) -> {
      if (value != null) {
        NotAllowed na = (NotAllowed) spec;
        if (value.toString().equals(na.value())) {
          violations.add(new Violation(Severity.ERROR, ownerOfField, property,
              "value " + value + " is not allowed", null));
        }
      }
      return value;
    });
  }

  static {
    registerNotNull();
    registerNotEmpty();
    registerLength();
    registerNoXss();
    registerRegexpPattern();
    registerMax();
    registerMin();
    registerBefore();
    registerAfter();
    registerUpperCase();
    registerLowerCase();
    registerCamelCase();
    registerSnakeCase();
    registerKebabCase();
    registerNotAllowed();
  }

  private static Instant convertToInstant(Object value) {
    if (value instanceof LocalDate time) {
      return Instant.from(time.atStartOfDay(ZoneId.systemDefault()));
    }
    if (value instanceof OffsetDateTime time) {
      return Instant.from(time);
    }
    if (value instanceof LocalDateTime time) {
      return Instant.from(time.atZone(ZoneId.systemDefault()));
    }
    throw new UnsupportedTemporalTypeException(value.getClass().getName());
  }

  private static Instant parseIsoInstant(String anyDateOrTime) {
    if (anyDateOrTime.equals("now")) {
      return Instant.now();
    }
    if (anyDateOrTime.length() == 8) {
      return Instant.from(LocalDate.parse(anyDateOrTime).atStartOfDay(ZoneId.systemDefault()));
    }
    if (anyDateOrTime.length() > 18) {
      if (anyDateOrTime.charAt(anyDateOrTime.length() - 6) == '+'
          || anyDateOrTime.charAt(anyDateOrTime.length() - 6) == '-') {
        return Instant.from(OffsetDateTime.parse(anyDateOrTime));
      }
      return Instant.from(LocalDateTime.parse(anyDateOrTime).atZone(ZoneId.systemDefault()));
    }
    throw new DateTimeParseException("Cannot parse value", anyDateOrTime, -1);
  }
}
