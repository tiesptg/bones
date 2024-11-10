package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.palisand.bones.di.Classes;
import com.palisand.bones.tt.Repository.Token;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObjectConverter implements Converter<Object> {
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
	
	@Data
	@NoArgsConstructor
	public static class Property {
		private String name;
		private Method getter;
		private Method setter;
		private Class<?> componentType;
		private Object defaultValue;
		
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
			return List.class.isAssignableFrom(getter.getReturnType());
		}
		
		public boolean isLink() {
			return Link.class.isAssignableFrom(getter.getReturnType());
		}
		
		public String getName() {
			String name = getLabel();
			return Character.toLowerCase(name.charAt(0)) + name.substring(1);
		}
		
		public String getLabel() {
			return setter.getName().substring(3);
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
	
	public ObjectConverter(Class<?> cls) {
		Object object = newInstance(cls);
		type = cls;
		List<Class<?>> classes = new ArrayList<>();
		while (cls != Node.class && cls != Object.class) {
			classes.add(0, cls);
			cls = cls.getSuperclass();
		}
		for (Class<?> clazz: classes) {
			for (Field field: clazz.getDeclaredFields()) {
				if (!Modifier.isVolatile(field.getModifiers())) {
					Property property = new Property();
					property.setName(field.getName());
					String label = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
					String getterName = (field.getType() == boolean.class || field.getType() == Boolean.class ? "is" : "get") + label;
					try {
						property.setGetter(clazz.getMethod(getterName));
						property.setSetter(clazz.getMethod("set" + label, field.getType()));
						if (!Node.class.isAssignableFrom(property.getType()) && !Link.class.isAssignableFrom(property.getType())
								&& !List.class.isAssignableFrom(property.getType())) {
							try {
								property.setDefaultValue(property.getGetter().invoke(object));
							} catch (Exception ex) {
								// ignore
							}
						}
						if (property.isList()) {
							property.setComponentType(Classes.getGenericType(field.getGenericType(), 0));
						} else if (property.isLink()) {
							property.setComponentType(Classes.getGenericType(field.getGenericType(), 1));
						}
						properties.add(property);
					} catch (NoSuchMethodException ex) {
						// ignore
					}
				}
			}
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
			if (property != null) {
				Converter<?> propertyConverter = repository.getConverter(property.getType());
				Object value = propertyConverter.fromTypedText(in, property.getComponentType(), newMargin);
				try {
					if (property.isLink()) {
						Link<?,?> link = (Link<?,?>)property.getter.invoke(result);
						link.setPath((String)value);
						link.setRepository(repository);
					} else {
						property.setter.invoke(result, value);
					}
				} catch (Exception ex) {
					throw new IOException(ex);
				}
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
		}
		repository.setContext(oldContext);
		return result;
	}
	
	private String getClassLabel(Class<?> cls) {
		String context = repository.getContext().getName().substring(0,repository.getContext().getName().length() - repository.getContext().getSimpleName().length());
		String check = cls.getName().substring(0,cls.getName().length()-cls.getSimpleName().length());
		if (context.equals(check)) {
			return cls.getSimpleName();
		}
		return cls.getName();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void toTypedText(Object obj, PrintWriter out, String margin) throws IOException {
		Class<?> oldContext = repository.getContext();
		if (obj == null) {
			out.println("null");
		} else if (obj instanceof Node node) {
			if (node.getContainer() != null) {
				repository.setContext(node.getContainer().getClass());
			}
		}
		out.print(getClassLabel(obj.getClass()));
		out.println('>');
		for (Property property: properties) {
			Object value = null;
			try {
				value = property.getGetter().invoke(obj);
				if (property.isLink()) {
					Link<?,?> link = (Link<?,?>)value;
					value = link.getPath();
					link.setRepository(repository);
				}
			} catch (Exception ex) {
				throw new IOException(ex);
			}
			String newMargin = margin + Repository.MARGIN_STEP;
			Converter<Object> converter = value == null ? (Converter<Object>)repository.getConverter(property.getType()) : (Converter<Object>)repository.getConverter(value.getClass());
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
		repository.setContext(oldContext);
	}

}
