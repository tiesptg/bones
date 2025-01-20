package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.palisand.bones.di.Classes;
import com.palisand.bones.tt.Repository.Token;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObjectConverter implements Converter<Object> {
	private static final Map<String,ObjectConverter> CONVERTERS = new TreeMap<>();
	@Getter private List<Property> properties = new ArrayList<>();
	@Getter private Method containerSetter = null;
	private Repository repository = null;
	@Getter private Class<?> type;
	
	public void setRepository(Repository mapper) {
		this.repository = mapper;
	}
	
	public Property getProperty(String name) {
		for (Property property: properties) {
			if (property.getName().equals(name)) {
				return property;
			}
		}
		return null;
	}
	
	public static ObjectConverter getConverter(Class<?> c) {
		ObjectConverter result = CONVERTERS.get(c.getName());
		if (result == null) {
			result = new ObjectConverter(c);
			CONVERTERS.put(c.getName(), result);
		}
		return result;
	}
	
	@Data
	@NoArgsConstructor
	public static class Property {
		private String name;
		private Method getter;
		private Method setter;
		private Class<?> componentType;
		private Object defaultValue;
		private Rules<?> rules;
		
		public Class<?> getType() {
			return getter.getReturnType();
		}
		
		public Class<?> getComponentType() {
			if (componentType != null) {
				return componentType;
			}
			return getType();
		}
		
		public boolean isList() {
			return List.class.isAssignableFrom(getter.getReturnType()) ||
					LinkList.class.isAssignableFrom(getter.getReturnType());
		}
		
		public boolean isLink() {
			return Link.class.isAssignableFrom(getter.getReturnType()) ||
					LinkList.class.isAssignableFrom(getter.getReturnType());
		}
		
		public boolean isReadonly() {
			return setter == null && !isLink() && !isList();
		}
		
		public boolean isTextIgnore() {
			return getter.getAnnotation(TextIgnore.class) != null || isReadonly();
		}
		
		public boolean hasTextIgnoreAnnotation() {
			return getter.getAnnotation(TextIgnore.class) != null;
		}
		
		public String getLabel() {
			return Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
		
		public Object getValue(Object object) {
			try {
				return getter.invoke(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		public boolean isDefault(Object value) {
			if (value == null && defaultValue == null) {
				return true;
			} 
			if (value instanceof LinkList linkList) {
				return linkList.isEmpty();
			}
			if (value instanceof List list) {
				return list.isEmpty();
			}
			if (value == null || defaultValue == null) {
				return false;
			}
			if (value.equals(defaultValue)) {
				return true;
			}
			return false;
		}
	}
	
	private boolean isGetter(Method method, StringBuilder name) {
		if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
			String prefix = "get";
			if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
				prefix = "is";
			}
			if (method.getName().startsWith(prefix)) {
				name.setLength(0);
				name.append(method.getName().substring(prefix.length()));
				return true;
			}
		}
		return false;
	}
	
	private ObjectConverter(Class<?> cls) {
		Object object = newInstance(cls);
		type = cls;
		StringBuilder name = new StringBuilder();
		int index = 0;
		Class<?> ref = cls;
		for (Method method: cls.getMethods()) {
			if (isGetter(method,name) && method.getDeclaringClass() != Object.class && method.getDeclaringClass() != Node.class) {
				name.insert(0, "set");
				Property property = new Property();
				property.setGetter(method);
				try {
					Method setter = cls.getMethod(name.toString(), method.getReturnType());
					if (!Modifier.isStatic(setter.getModifiers()) && setter.getAnnotation(TextIgnore.class) == null) {
						property.setSetter(setter);
					}
				} catch (NoSuchMethodException ex) {
					// ignore
				}
				name.delete(0, 3);
				name.setCharAt(0, Character.toLowerCase(name.charAt(0)));
				property.setName(name.toString());
				if (object instanceof Node<?> node) {
					property.setRules(node.getConstraint(name.toString()));
				}
				if (!Node.class.isAssignableFrom(property.getType()) && !Link.class.isAssignableFrom(property.getType())
						&& !List.class.isAssignableFrom(property.getType())) {
					try {
						property.setDefaultValue(property.getGetter().invoke(object));
					} catch (Exception ex) {
						// ignore
					}
				}
				if (property.isLink()) {
					property.setComponentType(Classes.getGenericType(method.getGenericReturnType(), 1));
				} else if (property.isList()) {
					property.setComponentType(Classes.getGenericType(method.getGenericReturnType(), 0));
				}
				if (ref != method.getDeclaringClass()) {
					index = 0;
					ref = method.getDeclaringClass();
				}
				properties.add(index++,property);
			}
		}
	}
		
	@SuppressWarnings("unchecked")
	private void linkFromTypedText(Object result, Property property, Object value) throws Exception {
		if (property.isList()) {
			List<String> list = (List<String>)value;
			LinkList<?,?> linkList = (LinkList<?,?>)property.getGetter().invoke(result);
			linkList.setRepository(repository);
			for (String path: list) {
				linkList.addPath(path);
			}
		} else {
			Link<?,?> link = (Link<?,?>)property.getter.invoke(result);
			link.setPath((String)value);
			link.setRepository(repository);
		}
	}
	
	@Override
	public Object fromTypedText(BufferedReader in, Class<?> cls, String margin) throws IOException {
		Token token = repository.nextToken(in);
		assert token.delimiter() == '>';
		Class<?> oldContext = repository.getContext();
		repository.setContext(type);
		ObjectConverter converter = (ObjectConverter)repository.getConverter(cls,token.label());
		repository.consumeLastToken();
		repository.readUntilLineEnd(in);
		
		Object result = newInstance(converter.type);
		String newMargin = margin + Repository.MARGIN_STEP;
		Property property = null;
		for (token = repository.nextToken(in); !isEnd(token,margin); token = repository.nextToken(in)) {
			property = converter.getProperty(token.label());
			repository.consumeLastToken();
			if (property != null && !property.isReadonly()) {
				Converter<?> propertyConverter = repository.getConverter(property.getType());
				Object value = propertyConverter.fromTypedText(in, property.getComponentType(), newMargin);
				try {
					if (property.isLink()) {
						linkFromTypedText(result,property,value);
					} else {
						property.setter.invoke(result, value);
					}
				} catch (Exception ex) {
					throw new IOException(ex);
				}
				if (!property.isLink()) {
					if (value instanceof Node<?> node) {
						node.setContainer((Node<?>)result,token.label());
					} else if (value instanceof List list) {
						for (Object item: list) {
							if (item instanceof Node<?> node) {
								node.setContainer((Node<?>)result,token.label());
							}
						}
					}
				}
			} else {
				String testMargin = token.margin();
				Converter<?> strConv = repository.getConverter(String.class);
				strConv.fromTypedText(in, String.class, newMargin);
				for (token = repository.nextToken(in); token != null && token.margin().length() > testMargin.length(); token = repository.nextToken(in)) {
					repository.consumeLastToken();
					strConv.fromTypedText(in, String.class, newMargin);
				}
			}
		}
		repository.setContext(oldContext);
		return result;
	}
	
	private String getClassLabel(Class<?> cls) {
		Package pack = repository.getContext().getPackage();
		if (pack.equals(cls.getPackage())) {
			return cls.getSimpleName();
		}
		return cls.getName();
	}
	
	private Object linkToTypedText(Object object, Property property, Object value) throws IOException {
		Object result = null;
		if (property.isList()) {
			LinkList<?,?> linkList = (LinkList<?,?>)value;
			List<String> list = new ArrayList<>();
			result = list;
			List<IOException> exList = new ArrayList<>();
			linkList.getList().forEach(link -> {
				try {
					list.add(link.getPath());
				} catch (IOException ioex) {
					exList.add(ioex);
				}
			});
			if (!exList.isEmpty()) {
				throw exList.get(0);
			}
			linkList.setRepository(repository);
		} else {
			Link<?,?> link = (Link<?,?>)value;
			result = link.getPath();
			link.setRepository(repository);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void toTypedText(Object obj, PrintWriter out, String margin) throws IOException {
		Class<?> oldContext = repository.getContext();
		if (obj == null) {
			out.println("null");
		} else {
			out.print(getClassLabel(obj.getClass()));
			out.println('>');
			repository.setContext(obj.getClass());
			for (Property property: properties) {
				if (!property.isTextIgnore()) {
					Object value = null;
					try {
						value = property.getGetter().invoke(obj);
						if (property.isLink()) {
							value = linkToTypedText(obj,property,value);
						}
					} catch (Exception ex) {
						throw new IOException(ex);
					}
					String newMargin = margin + Repository.MARGIN_STEP;
					Converter<Object> converter = value == null ? (Converter<Object>)repository.getConverter(property.getType())
							: (Converter<Object>)repository.getConverter(value.getClass());
					if (!property.isDefault(value)) {
						out.print(margin);
						out.print(property.getName());
						out.print(":\t");
						if (converter != null) {
							converter.toTypedText(value, out, newMargin);
						} else {
							out.println("null");
						}
					}
				}
			}
			repository.setContext(oldContext);
		}
	}
	
}
