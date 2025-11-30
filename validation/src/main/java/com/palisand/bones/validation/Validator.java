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
import java.util.function.Predicate;
import com.palisand.bones.validation.Rules.Rule;

public class Validator {
  private final static Map<Class<?>, Rule> RULES = new HashMap<>();
  private final Map<Class<?>, Property<?>[]> classCache = new HashMap<>();

  static {
    Rules.register();
  }

  public static <A extends Annotation> void addRule(Class<A> annotationClass, Rule rule) {
    RULES.put(annotationClass, rule);
  }

  private record Property<X>(Field field, Method getter, Method setter, Predicate<X> validWhen,
      Map<Rule, ?> rules) {

    void setToDefault(Object object) throws Exception {
      if (!field.getType().isPrimitive()) {
        setter.invoke(object, (Object) null);
      }
    }
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

  @SuppressWarnings("unchecked")
  private <X> void scanClass(List<Property<X>> properties, Class<X> cls) throws Exception {
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
      ValidWhen validWhen = field.getAnnotation(ValidWhen.class);
      Predicate<X> predicate = null;
      if (validWhen != null) {
        predicate = (Predicate<X>) validWhen.value().getDeclaredConstructor().newInstance();
      }
      properties.add(new Property<X>(field, getter, setter, predicate, found));
    }
    if (cls.getSuperclass() != Object.class) {
      scanClass(properties, (Class<X>) cls.getSuperclass());
    }
  }

  @SuppressWarnings("unchecked")
  private <X> Property<X>[] getProperties(Class<X> cls) throws Exception {
    Property<X>[] properties = (Property<X>[]) classCache.get(cls);
    if (properties == null) {
      List<Property<X>> result = new ArrayList<>();
      scanClass(result, cls);
      properties = result.toArray(size -> new Property[size]);
      classCache.put(cls, properties);
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

  @SuppressWarnings("unchecked")
  private <X> void validateBean(List<Violation> result, Set<Object> cycleChecker, X object) {
    try {
      for (Property<X> property : (Property<X>[]) getProperties(object.getClass())) {
        if (property.validWhen() == null || property.validWhen().test(object)) {
          Object fieldValue = property.getter().invoke(object);
          for (Entry<Rule, ?> entry : property.rules().entrySet()) {
            Object newValue = entry.getKey().validate(result, entry.getValue(),
                property.field().getName(), fieldValue);
            if (newValue != fieldValue || (fieldValue != null && !fieldValue.equals(newValue))) {
              property.setter().invoke(object, newValue);
              fieldValue = newValue;
            }
          }
          validateObject(result, cycleChecker, fieldValue);
        } else {
          property.setToDefault(object);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      result.add(new Violation(object.getClass().getName(),
          "Exception while checking value: " + e.toString(), object, null));
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
