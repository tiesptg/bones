package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.palisand.bones.tt.Repository.Token;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObjectConverter implements Converter<Object> {
	@Getter private Map<String,Property> properties = new TreeMap<>();
	@Getter private Method containerSetter = null;
	private Repository mapper = null;
	@Getter private Class<?> type;
	
	public void setMapper(Repository mapper) {
		this.mapper = mapper;
	}
	
	@Data
	@NoArgsConstructor
	public static class Property {
		private Method getter;
		private Method setter;
		private Class<?> componentType;
		private Object defaultValue;
		private boolean link;
		
		public Class<?> getType() {
			if (componentType != null) {
				return componentType;
			}
			return getter.getReturnType();
		}
		
		public boolean isList() {
			return componentType != null;
		}
	}
	
	private String getPropertyName(String name) {
		if (name.startsWith("is")) {
			return Character.toLowerCase(name.charAt(2)) + name.substring(3);
		}
		return Character.toLowerCase(name.charAt(3)) + name.substring(4);
	}
	
	private String isGetter(Method method) {
		if (method.getParameterCount() == 0 && (method.getName().startsWith("get")
				|| (method.getName().startsWith("is") && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)))) {
			return getPropertyName(method.getName());
		}
		return null;
	}
	
	private String isSetter(Method method) {
		if (method.getParameterCount() == 1 && method.getName().startsWith("set")) {
			return getPropertyName(method.getName());
		}
		return null;
	}
	
	private Property getProperty(String name) {
		Property property = properties.get(name);
		if (property == null) {
			property = new Property();
			properties.put(name, property);
		}
		return property;
	}
	
	private Class<?> getListType(Type type) {
		ParameterizedType pType = (ParameterizedType)type;
        Type rType = pType.getActualTypeArguments()[0];
        if (rType instanceof Class<?> cls) {
        	return cls;
        }
        if (rType instanceof ParameterizedType prtype) {
        	return (Class<?>)prtype.getRawType();
        }
        return null;
	}
	
	public ObjectConverter(Class<?> cls) {
		Object object = newInstance(cls);
		type = cls;
		while (cls != Node.class && cls != Object.class) {
			for (Method method: cls.getDeclaredMethods()) {
				String name = isGetter(method);
				if (name != null) {
					Property property = getProperty(name);
					property.setGetter(method);
					try {
						Field field = cls.getDeclaredField(name);
						property.setLink(Link.class.isAssignableFrom(field.getType()));
					} catch (NoSuchFieldException ex) {
						// ignore
					}
					try {
						if (!property.isLink() && !property.isList()) {
							property.setDefaultValue(method.invoke(object));
						}
						if (List.class.isAssignableFrom(property.getType())) {
							property.setComponentType(getListType(property.getGetter().getGenericReturnType()));
						} else if (property.isLink()) {
							property.setComponentType(Link.class);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						throw new UnsupportedOperationException(cls + " does not have a public default constructor");
					}
				} else {
					name = isSetter(method);
					if (name != null) {
						Property property = getProperty(name);
						property.setSetter(method);
					}
				}
			}
			cls = cls.getSuperclass();
		}
		// remove incomplete properties
		List<String> toRemove = properties.entrySet().stream()
				.filter(e -> !e.getValue().isLink() && (e.getValue().getSetter() == null || e.getValue().getGetter() == null))
				.map(e -> e.getKey()).toList();
		toRemove.forEach(name -> properties.remove(name));
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
			property = converter.getProperties().get(token.label());
			mapper.consumeLastToken();
			Converter<?> propertyConverter = mapper.getConverter(property.isList() ? List.class : property.getType());
			Object value = propertyConverter.fromYaml(in, property.getType(), newMargin);
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
		if (obj == null) {
			out.println("null");
		} else if (obj instanceof Node node) {
			Class<?> oldContext = mapper.getContext();
			if (node.getContainer() != null) {
				mapper.setContext(node.getContainer().getClass());
			}
			out.print(getClassLabel(obj.getClass()));
			out.println('>');
			for (Entry<String,Property> entry: properties.entrySet()) {
				Object value = null;
				try {
					value = entry.getValue().getGetter().invoke(obj);
					if (entry.getValue().isLink()) {
						Link<?,?> link = (Link<?,?>)value;
						value = link.getPath();
						link.setMapper(mapper);
					}
				} catch (Exception ex) {
					throw new IOException(ex);
				}
				Converter<Object> converter = (Converter<Object>)mapper.getConverter(entry.getValue().isList() ? List.class : entry.getValue().getType());
				if (!equalsValue(value,entry.getValue().getDefaultValue())) {
					out.print(margin);
					out.print(entry.getKey());
					out.print(":\t");
					converter.toYaml(value, out, margin);
				}
			}
			mapper.setContext(oldContext);
		} else {
			throw new UnsupportedOperationException("This Converter only supports objects derived from Node");
		}
	}

}
