package com.palisand.bones;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import com.palisand.bones.validation.Rules;
import com.palisand.bones.validation.Rules.PredicateWithException;
import com.palisand.bones.validation.Rules.Rule;
import com.palisand.bones.validation.ValidWhen;
import lombok.Data;

/**
 * Utility class for classpath scanning and reflection
 */
public class Classes {
  private static final Map<Class<?>, List<Property<?>>> CLASS_CACHE = new HashMap<>();

  @FunctionalInterface
  public interface Creator<X, Y extends Property<X>> {
    Y newProperty() throws Exception;
  }

  @FunctionalInterface
  public interface Extender<X, Y extends Property<X>> {
    void extend(Y y) throws Exception;
  }

  /**
   * Get Class of the generic parameter of the given type
   * 
   * @param type the type whose generic parameter type will be returned
   * @param position the position of the parameter (0 is first)
   * @return the Class of the genetic parameter of the given type
   */
  public static Class<?> getGenericType(Type type, int position) {
    try {
      ParameterizedType pType = (ParameterizedType) type;
      Type typeResult = pType.getActualTypeArguments()[position];
      if (typeResult instanceof Class cls) {
        return cls;
      }
      if (typeResult instanceof ParameterizedType rType) {
        return (Class<?>) rType.getRawType();
      }
    } catch (Exception ex) {
      // ignore
    }
    return null;
  }

  /**
   * Gives the list of classes found in the classpath that fullfile the given condition
   * 
   * @param prefix The prefix the full name of the classes should have (you can use the name of the
   *        package or a part of it) or an empty string
   * @param condition A function that tests classes to be included in the result
   * @return A list of classes whose full name starts with the prefix and fulfills the condition
   * @throws IOException
   */
  public static List<Class<?>> findClasses(String prefix, Function<Class<?>, Boolean> condition)
      throws IOException {
    String[] parts = System.getProperty("java.class.path").split(File.pathSeparator);
    ClassLoader loader = Classes.class.getClassLoader();
    ArrayList<Class<?>> result = new ArrayList<>();
    for (String str : parts) {
      if (str.toLowerCase().endsWith("jar")) {
        result.addAll(findClassesInJar(loader, str, prefix, condition));
      } else {
        result.addAll(findClassesInDir(loader, str, prefix, condition));
      }
    }
    return result;
  }

  private static List<Class<?>> findClassesInJar(ClassLoader cl, String file, String prefix,
      Function<Class<?>, Boolean> condition) throws IOException {
    List<Class<?>> result = new ArrayList<>();
    try (JarFile jar = new JarFile(file)) {
      Enumeration<JarEntry> e = jar.entries();
      while (e.hasMoreElements()) {
        JarEntry entry = e.nextElement();
        if (!entry.isDirectory() && entry.getName().endsWith(".class")
            && entry.getName().startsWith(prefix)) {
          try {
            String name = entry.getName();
            name = name.substring(0, name.length() - 6);
            name = name.replace("/", ".");
            Class<?> cls = Class.forName(name);
            if (condition.apply(cls)) {
              result.add(cls);
            }
          } catch (Throwable e2) {
            // ignore
          }
        }
      }
      return result;
    }
  }

  private static void getAllClassNames(List<String> result, File dir, String from,
      String prefixTest) {
    if (from.startsWith(prefixTest) || prefixTest.startsWith(from)) {
      for (File child : dir.listFiles()) {
        if (child.isDirectory()) {
          getAllClassNames(result, child,
              from.isEmpty() ? child.getName() : from + "." + child.getName(), prefixTest);
        } else if (child.isFile() && child.getName().endsWith(".class")) {
          String newname = from.isEmpty() ? child.getName() : from + "." + child.getName();
          if (newname.startsWith(prefixTest)) {
            result.add(newname);
          }
        }
      }
    }
  }

  private static List<Class<?>> findClassesInDir(ClassLoader cl, String file, String prefix,
      Function<Class<?>, Boolean> condition) {
    List<Class<?>> result = new ArrayList<>();
    List<String> strList = new ArrayList<>();
    getAllClassNames(strList, new File(file), "", prefix);
    for (String name : strList) {
      // remove .class extension
      name = name.substring(0, name.length() - 6);
      try {
        Class<?> cls = Class.forName(name);
        if (condition.apply(cls)) {
          result.add(cls);
        }
      } catch (Throwable e1) {
        // ignore
      }
    }
    return result;
  }

  @Data
  public static class Property<X> {
    private Field field;
    private Method getter;
    private Method setter;
    private Object defaultValue;
    private PredicateWithException<X> validWhen;
    private Map<Rule, ? extends Annotation> rules;

    public boolean isReadonly() {
      return setter == null;
    }

    public boolean is(Class<? extends Annotation> cls) {
      return field.getAnnotation(cls) != null;
    }

    public void setToDefault(Object object) throws Exception {}

    public Object get(Object owner) {
      try {
        return getter.invoke(owner);
      } catch (Exception ex) {
        throw new IllegalArgumentException(ex);
      }
    }

    public void set(Object owner, Object value) {
      try {
        if (setter != null) {
          setter.invoke(owner, value);
        }
      } catch (Exception ex) {
        throw new IllegalArgumentException(ex);
      }
    }

  }

  private static Method getGetter(Class<?> cls, Field field) {
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

  private static Method getSetter(Class<?> cls, Field field, Class<?> type) {
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

  private static Object getDefaultValue(Object instance, Method getter) {
    try {
      return getter.invoke(instance);
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  private static <X, Y extends Property<X>> void scanClass(List<Y> properties, Class<?> cls,
      X instance, Creator<X, Y> creator, Extender<X, Y> extender) throws Exception {
    for (Field field : cls.getDeclaredFields()) {
      Y property = creator.newProperty();
      property.setGetter(getGetter(cls, field));
      property.setSetter(getSetter(cls, field, field.getType()));
      property.setDefaultValue(
          property.getSetter() != null ? getDefaultValue(instance, property.getGetter()) : null);
      ValidWhen validWhen = field.getAnnotation(ValidWhen.class);
      property.setValidWhen(validWhen == null ? null
          : (PredicateWithException<X>) validWhen.value().getDeclaredConstructor().newInstance());
      Map<Rule, Annotation> rules = new HashMap<>();
      for (Annotation spec : field.getAnnotations()) {
        Rule rule = Rules.getRule(spec.annotationType());
        if (rule != null) {
          rules.put(rule, spec);
        }
      }
      property.setRules(rules);
      extender.extend(property);
      properties.add(property);
    }
    if (cls.getSuperclass() != Object.class) {
      scanClass(properties, cls.getSuperclass(), instance, creator, extender);
    }
  }

  public static <X> List<Property<X>> getProperties(Class<X> cls) throws Exception {
    return getProperties(cls, () -> new Property<X>(), object -> {
    });
  }

  @SuppressWarnings("unchecked")
  public static <X, Y extends Property<X>> List<Y> getProperties(Class<X> cls,
      Creator<X, Y> creator, Extender<X, Y> extender) throws Exception {
    List<Y> properties = (List<Y>) CLASS_CACHE.get(cls);
    if (properties == null) {
      properties = new ArrayList<>();
      if (cls != Object.class) {
        X instance = cls.getDeclaredConstructor().newInstance();
        scanClass(properties, cls, instance, creator, extender);
      }
      CLASS_CACHE.put(cls, (List<Property<?>>) properties);
    }
    return properties;
  }

}
