package com.palisand.bones.tt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import com.palisand.bones.tt.Rules.ConstraintViolation;
import com.palisand.bones.tt.Rules.Severity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Validator {
  private Node<?> node;
  private List<ConstraintViolation> violations = new ArrayList<>();

  public void assertNotNull(String field, Object value) {
    if (value == null) {
      violations.add(new ConstraintViolation(Severity.ERROR, node, field,
          "Field " + field + " should not be null", null));
    }
  }

  public void assertNull(String field, Object value) {
    if (value != null) {
      violations.add(new ConstraintViolation(Severity.ERROR, node, field,
          "Field " + field + " should null", null));
    }
  }

  public void assertTrue(String field, boolean value, String message) {
    if (!value) {
      addViolation(field, message);
    }
  }

  public void addViolation(String field, Exception exception) {
    violations
        .add(new ConstraintViolation(Severity.ERROR, node, field, exception.toString(), exception));
  }

  public void addViolation(String field, String message) {
    violations.add(new ConstraintViolation(Severity.ERROR, node, field, message, null));
  }

  public void addWarning(String field, String message) {
    violations.add(new ConstraintViolation(Severity.WARNING, node, field, message, null));
  }

  public boolean containsErrors() {
    return violations.stream().anyMatch(violation -> violation.severity() == Severity.ERROR);
  }

  @FunctionalInterface
  public interface PredicateThatThrows<T> {
    boolean test(T t) throws Exception;
  }

  public <T> Predicate<T> checkException(String field, PredicateThatThrows<T> predicate) {
    return (T t) -> {
      try {
        return predicate.test(t);
      } catch (Exception ex) {
        addViolation(field, ex.toString());
      }
      return false;
    };
  }

}
