package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.palisand.bones.di.Classes;
import com.palisand.bones.tt.Repository.Token;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObjectConverter implements Converter<Object> {
	@Getter private List<Property> properties = new ArrayList<>();
	@Getter private Method containerSetter = null;
	private Repository mapper = null;
	@Getter private Class<?> type;
	
	public void setMapper(Repository mapper) {
		this.mapper = mapper;
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
	}
	
	public ObjectConverter(Class<?> cls) {
		Object object = newInstance(cls);
		type = cls;
		while (cls != Node.class && cls != Object.class) {
			for (Field field: cls.getDeclaredFields()) {
				if (!Modifier.isVolatile(field.getModifiers())) {
					Property property = new Property();
					property.setName(field.getName());
					String label = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
					String getterName = (field.getType() == boolean.class || field.getType() == Boolean.class ? "is" : "get") + label;
					try {
						property.setGetter(cls.getMethod(getterName));
						property.setSetter(cls.getMethod("set" + label, field.getType()));
						if (object != null) {
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
			cls = cls.getSuperclass();
		}
		// remove incomplete properties
	}
	
	@Override
	public Object fromYaml(BufferedReader in, Class<?> cls, String margin) throws IOException {
		Token token = mapper.nextToken(in);
		assert token.delimiter() == '>';
		Class<?> oldContext = mapper.getContext();
		mapper.setContext(type);
		ObjectConverter converter = (ObjectConverter)mapper.getConverter(cls,token.label());
		mapper.consumeLastToken();
		mapper.readUntilLineEnd(in);
		
		Object result = newInstance(converter.type);
		String newMargin = margin + Repository.MARGIN_STEP;
		Property property = null;
		for (token = mapper.nextToken(in); !isEnd(token,margin); token = mapper.nextToken(in)) {
			property = converter.getProperty(token.label());
			mapper.consumeLastToken();
			if (property != null) {
				Converter<?> propertyConverter = mapper.getConverter(property.getType());
				Object value = propertyConverter.fromYaml(in, property.getComponentType(), newMargin);
				try {
					if (property.isLink()) {
						Link<?,?> link = (Link<?,?>)property.getter.invoke(result);
						link.setPath((String)value);
						link.setMapper(mapper);
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
		mapper.setContext(oldContext);
		return result;
	}
	
	private boolean equalsValue(Object value, Object defaultValue) {
		if (value == null && defaultValue == null) return true;
		if (value == null || defaultValue == null) return false;
		return value.equals(defaultValue);
	}
	
	private String getClassLabel(Class<?> cls) {
		String context = mapper.getContext().getName().substring(0,mapper.getContext().getName().length() - mapper.getContext().getSimpleName().length());
		String check = cls.getName().substring(0,cls.getName().length()-cls.getSimpleName().length());
		if (context.equals(check)) {
			return cls.getSimpleName();
		}
		return cls.getName();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void toYaml(Object obj, PrintWriter out, String margin) throws IOException {
		Class<?> oldContext = mapper.getContext();
		if (obj == null) {
			out.println("null");
		} else if (obj instanceof Node node) {
			if (node.getContainer() != null) {
				mapper.setContext(node.getContainer().getClass());
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
					link.setMapper(mapper);
				}
			} catch (Exception ex) {
				throw new IOException(ex);
			}
			Converter<Object> converter = value == null ? null : (Converter<Object>)mapper.getConverter(property.isList() ? List.class : property.getType());
			if (!equalsValue(value,property.getDefaultValue())) {
				out.print(margin);
				out.print(property.getName());
				out.print(":\t");
				if (converter != null) {
					converter.toYaml(value, out, margin);
				} else {
					out.println("null");
				}
			}
		}
		mapper.setContext(oldContext);
	}

}
