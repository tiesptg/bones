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

public class Classes {
	
	public static Class<?> getGenericType(Type type, int position) {
		try {
			ParameterizedType pType = (ParameterizedType)type;
	        return(Class<?>)pType.getActualTypeArguments()[position];
		} catch (Exception ex) {
			// ignore
		}
		return null;
	}
	

	
	public static List<Class<?>> findClasses(String prefix,Function<Class<?>,Boolean> condition) throws IOException {
		String[] parts = System.getProperty("java.class.path").split(File.pathSeparator);
		ClassLoader loader = Classes.class.getClassLoader();
		ArrayList<Class<?>> result = new ArrayList<>();
		for (String str: parts) {
			if (str.toLowerCase().endsWith("jar")) {
				result.addAll(findClassesInJar(loader,str,prefix, condition));
			} else {
				result.addAll(findClassesInDir(loader,str,prefix, condition));
			}
		}
		return result;
	}
	
	private static List<Class<?>> findClassesInJar(ClassLoader cl,String file, String prefix, Function<Class<?>,Boolean> condition) throws IOException {
		List<Class<?>> result = new ArrayList<>();
		try (JarFile jar = new JarFile(file)) {
			Enumeration<JarEntry> e = jar.entries();
			while (e.hasMoreElements()) {
				JarEntry entry = e.nextElement();
				if (!entry.isDirectory() && entry.getName().endsWith(".class") 
						&& entry.getName().startsWith(prefix)) {
					try {
						String name = entry.getName();
						name = name.substring(0,name.length()-6);
						name = name.replace("/", ".");
						Class<?> cls = Class.forName(name);
						if (condition.apply(cls)) {
							result.add(cls);
						}
					} catch (ClassNotFoundException | NoClassDefFoundError e2) {
						// ignore
					} 
				}
			}
			return result;
		}
	}
	
	private static void getAllClassNames(List<String> result, File dir, String from, String prefixTest) {
		if (from.startsWith(prefixTest) || prefixTest.startsWith(from)) {
			for (File child: dir.listFiles()) {
				if (child.isDirectory()) {
					getAllClassNames(result,child,from.isEmpty() ? child.getName() : from + "." + child.getName(),prefixTest);
				} else if (child.isFile() && child.getName().endsWith(".class")) {
					String newname = from.isEmpty() ? child.getName() : from + "." + child.getName();
					if (newname.startsWith(prefixTest)) {
						result.add(newname);
					}
				}
			}
		}
	}
	
	private static List<Class<?>> findClassesInDir(ClassLoader cl, String file, String prefix, Function<Class<?>,Boolean> condition) {
		List<Class<?>> result = new ArrayList<>();
		List<String> strList = new ArrayList<>();
		getAllClassNames(strList,new File(file),"",prefix);
		for (String name: strList) {
			// remove .class extension
			name = name.substring(0,name.length()-6);
			try {
				Class<?> cls = Class.forName(name);
				if (condition.apply(cls)) {
					result.add(cls);
				}
			} catch (ClassNotFoundException e1) {
				// ignore
			} 
		}
		return result;
	}
	
}
