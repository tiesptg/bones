package com.palisand.bones.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.palisand.bones.Classes;
import com.palisand.bones.Classes.Property;
import com.palisand.bones.validation.Rules.Rule;
import com.palisand.bones.validation.Rules.Severity;
import com.palisand.bones.validation.Rules.Violation;

public class Validator {

  private boolean isLeaf(Object obj) {
    return obj == null || obj.getClass().isPrimitive()
        || (obj.getClass().getName().startsWith("java")
            && !Collection.class.isAssignableFrom(obj.getClass())
            && !Map.class.isAssignableFrom(obj.getClass()));
  }

  @SuppressWarnings("unchecked")
  private <X> void validateBean(List<Violation> result, Set<Object> cycleChecker, X object) {
    Property<X> ref = null;
    try {
      List<Property<X>> properties = Classes.getProperties((Class<X>) object.getClass());
      for (Property<X> property : properties) {
        ref = property;
        if (property.getValidWhen() == null || property.getValidWhen().test(object)) {
          Object fieldValue = property.get(object);
          for (Entry<Rule, ?> entry : property.getRules().entrySet()) {
            Object newValue =
                entry.getKey().validate(result, object, entry.getValue(), property, fieldValue);
            if (newValue != fieldValue || (fieldValue != null && !fieldValue.equals(newValue))) {
              property.set(object, newValue);
              fieldValue = newValue;
            }
          }
          if (object instanceof Validatable valid) {
            valid.doValidate(result, (List<Property<?>>) (Object) properties);
          }
          validateObject(result, cycleChecker, fieldValue);
        } else {
          property.setToDefault(object);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      result.add(new Violation(Severity.ERROR, object, ref,
          "Exception while checking value: " + e.toString(), e));
    }
  }

  private void validateCollection(List<Violation> result, Set<Object> cycleChecker,
      Collection<?> object) {
    object.forEach(item -> {
      validateObject(result, cycleChecker, item);
    });
  }

  private void validateMap(List<Violation> result, Set<Object> cycleChecker, Map<?, ?> object) {
    object.entrySet().forEach(e -> {
      validateObject(result, cycleChecker, e.getKey());
      validateObject(result, cycleChecker, e.getValue());
    });
  }

  private void validateObject(List<Violation> result, Set<Object> cycleChecker, Object object) {
    if (!isLeaf(object) && !cycleChecker.contains(object)) {
      cycleChecker.add(object);
      if (Collection.class.isAssignableFrom(object.getClass())) {
        validateCollection(result, cycleChecker, (Collection<?>) object);
      } else if (Map.class.isAssignableFrom(object.getClass())) {
        validateMap(result, cycleChecker, (Map<?, ?>) object);
      } else {
        validateBean(result, cycleChecker, object);
      }
    }
  }

  public List<Violation> validate(Object object) {
    List<Violation> result = new ArrayList<>();
    Set<Object> cycleChecker = new HashSet<>();
    validateObject(result, cycleChecker, object);
    return result;
  }

}
