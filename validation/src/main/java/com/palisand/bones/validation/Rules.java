package com.palisand.bones.validation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Collection;
import java.util.List;
import com.palisand.bones.validation.Validator.Violation;

public class Rules {

  @FunctionalInterface
  public interface Rule {
    Object validate(List<Violation> violations, Object spec, String fieldName, Object value);
  }


  private static void registerNotNull() {
    Validator.addRule(NotNull.class, (violations, spec, fieldName, value) -> {
      if (value == null) {
        violations.add(new Violation(fieldName, "value should not be null", value, spec));
      }
      return value;
    });
  }

  private static void registerNotEmpty() {
    Validator.addRule(NotEmpty.class, (violations, spec, fieldName, value) -> {
      if (value == null) {
        violations.add(new Violation(fieldName, "value should not be null", value, spec));
      }
      if (value instanceof Collection collection) {
        if (collection.isEmpty()) {
          violations.add(new Violation(fieldName, "value should not be empty", value, spec));
        }
      }
      return value;
    });
  }

  private static void registerNoXss() {
    Validator.addRule(NoXss.class, (violations, spec, fieldName, value) -> {
      if (value != null) {
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
            case '&':
              sb.replace(i, ++i, "&amp;");
              i += 3;
              break;
          }
        }
        return sb.toString();
      }
      return value;

    });
  }

  private static void registerRegexpPattern() {
    Validator.addRule(RegexpPattern.class, (violations, spec, fieldName, value) -> {
      if (value != null) {
        RegexpPattern pattern = (RegexpPattern) spec;
        if (value.toString().matches(pattern.value())) {
          violations.add(new Violation(fieldName,
              "value does not conform to pattern :'" + pattern.value() + "'", value, spec));
        }
      }
      return value;
    });
  }

  private static void registerMax() {
    Validator.addRule(Max.class, (violations, spec, fieldName, value) -> {
      if (value != null) {
        Max max = (Max) spec;
        Number number = (Number) value;
        if ((max.inclusive() && number.doubleValue() > max.value())
            || (!max.inclusive() && number.doubleValue() >= max.value())) {
          violations.add(new Violation(fieldName, "number is too large", value, spec));
        }
      }
      return value;
    });
  }

  private static void registerMin() {
    Validator.addRule(Min.class, (violations, spec, fieldName, value) -> {
      if (value != null) {
        Min min = (Min) spec;
        Number number = (Number) value;
        if ((min.inclusive() && number.doubleValue() < min.value())
            || (!min.inclusive() && number.doubleValue() <= min.value())) {
          violations.add(new Violation(fieldName, "number is too small", value, spec));
        }
      }
      return value;
    });
  }

  private static void registerBefore() {
    Validator.addRule(Before.class, (violations, spec, fieldName, value) -> {
      if (value != null) {
        Before before = (Before) spec;
        Instant instant = parseIsoInstant(before.value());
        Instant timestamp = convertToInstant(value);
        if (timestamp.isAfter(instant)) {
          violations.add(
              new Violation(fieldName, "value should be before " + before.value(), value, spec));
        }
      }
      return value;
    });
  }

  private static void registerAfter() {
    Validator.addRule(After.class, (violations, spec, fieldName, value) -> {
      if (value != null) {
        After after = (After) spec;
        Instant instant = parseIsoInstant(after.value());
        Instant timestamp = convertToInstant(value);
        if (timestamp.isBefore(instant)) {
          violations.add(
              new Violation(fieldName, "value should be before " + after.value(), value, spec));
        }
      }
      return value;
    });
  }

  private static void registerUpperCase() {
    Validator.addRule(UpperCase.class, (violations, spec, fieldName, value) -> {
      return value.toString().toUpperCase();
    });
  }

  private static void registerLowerCase() {
    Validator.addRule(UpperCase.class, (violations, spec, fieldName, value) -> {
      return value.toString().toLowerCase();
    });
  }

  static void register() {
    registerNotNull();
    registerNotEmpty();
    registerNoXss();
    registerRegexpPattern();
    registerMax();
    registerMin();
    registerBefore();
    registerAfter();
    registerUpperCase();
    registerLowerCase();
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
