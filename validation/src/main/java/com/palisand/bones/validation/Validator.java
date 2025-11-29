package com.palisand.bones.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.palisand.bones.validation.Rules.Rule;

public class Validator {
  private final static Map<Class<?>, Rule> RULES = new HashMap<>();
  private final Map<Class<?>, Property[]> classCache = new HashMap<>();

  static {
    Rules.register();
  }

  public static <A extends Annotation> void addRule(Class<A> annotationClass, Rule rule) {
    RULES.put(annotationClass, rule);
  }

  private record Property(Field field, Method getter, Method setter, Map<Rule, ?> rules) {

  }

  private Method getGetter(Class<?> cls, Field field) {
    String postFix = field.getName();
    postFix = Character.toUpperCase(postFix.charAt(0)) + postFix.substring(1);
    Method result = null;
    try {
      result = cls.getMethod("get" + postFix);
      return result;
    } catch (NoSuchMethodException e) {
      try {
        result = cls.getMethod("is" + postFix);
        if (result.getReturnType() == boolean.class || result.getReturnType() == Boolean.class) {
          return result;
        }
      } catch (NoSuchMethodException e1) {
        // ignore
      }
    }
    return null;
  }

  private Method getSetter(Class<?> cls, Field field, Class<?> type) {
    String postFix = field.getName();
    postFix = Character.toUpperCase(postFix.charAt(0)) + postFix.substring(1);
    Method result = null;
    try {
      result = cls.getMethod("set" + postFix, type);
      return result;
    } catch (NoSuchMethodException e) {
    }
    return null;
  }

  private void scanClass(List<Property> properties, Class<?> cls) {
    for (Field field : cls.getDeclaredFields()) {
      Map<Rule, Object> found = new HashMap<>();
      Arrays.stream(field.getAnnotations()).forEach(anno -> {
        Rule rule = RULES.get(anno.annotationType());
        if (rule != null) {
          found.put(rule, anno);
        }
      });
      Method getter = getGetter(cls, field);
      Method setter = getSetter(cls, field, field.getType());
      properties.add(new Property(field, getter, setter, found));
    }
    if (cls.getSuperclass() != Object.class) {
      scanClass(properties, cls.getSuperclass());
    }
  }

  private Property[] getProperties(Class<?> cls) {
    Property[] properties = classCache.get(cls);
    if (properties == null) {
      List<Property> result = new ArrayList<>();
      scanClass(result, cls);
      properties = result.toArray(size -> new Property[size]);
    }
    return properties;
  }

  public record Violation(String field, String message, Object offendingValue, Object annotation) {

  }

  private boolean isLeaf(Object obj) {
    return obj == null || obj.getClass().isPrimitive()
        || (obj.getClass().getName().startsWith("java")
            && !Collection.class.isAssignableFrom(obj.getClass())
            && !Map.class.isAssignableFrom(obj.getClass()));
  }

  private void validateBean(List<Violation> result, Set<Object> cycleChecker, Object object) {
    for (Property property : getProperties(object.getClass())) {
      try {
        Object fieldValue = property.getter().invoke(object);
        for (Entry<Rule, ?> entry : property.rules().entrySet()) {
          Object newValue = entry.getKey().validate(result, entry.getValue(),
              property.field().getName(), fieldValue);
          if (newValue != fieldValue || !fieldValue.equals(newValue)) {
            property.setter().invoke(object, newValue);
            fieldValue = newValue;
          }
        }
        validateObject(result, cycleChecker, fieldValue);
      } catch (Exception e) {
        result.add(new Violation(property.field().getName(),
            "Exception while checking value: " + e.toString(), object, null));
      }
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
