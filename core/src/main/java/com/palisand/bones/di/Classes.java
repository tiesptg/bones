package com.palisand.bones.di;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class for classpath scanning and reflection
 */
public class Classes {

  /**
   * Get Class of the generic parameter of the given type
   * 
   * @param type the type whose generic parameter type will be returned
   * @param position the position of the parameter (0 is first)
   * @return the Class of the generatic parameter of the given type
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

}
