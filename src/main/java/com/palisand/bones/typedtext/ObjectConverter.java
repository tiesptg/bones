package com.palisand.bones.typedtext;

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

import com.palisand.bones.typedtext.Mapper.Token;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObjectConverter implements Converter<Node> {
	@Getter private Map<String,Property> properties = new TreeMap<>();
	@Getter private Method containerSetter = null;
	private Mapper mapper = null;
	@Getter private Class<?> type;
	
	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}
	
	@Data
	@NoArgsConstructor
	class Property {
		private Method getter;
		private Method setter;
		private Class<?> componentType;
		Object defaultValue;
		
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
        return(Class<?>)pType.getActualTypeArguments()[0];
	}
	
	public ObjectConverter(Class<?> cls) {
		type = cls;
		while (cls != Node.class) {
			for (Method method: cls.getDeclaredMethods()) {
				String name = isGetter(method);
				if (name != null) {
					Property property = getProperty(name);
					property.setGetter(method);
					try {
						Object object = newInstance(cls);
						property.setDefaultValue(method.invoke(object));
						if (List.class.isAssignableFrom(property.getType())) {
							property.setComponentType(getListType(property.getGetter().getGenericReturnType()));
						}
					} catch (UnsupportedOperationException ex) {
						throw ex;
					} catch (Exception ex) {
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
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		Token token = mapper.nextToken(in);
		assert token.delimiter() == '>';
		Class<?> oldContext = mapper.getContext();
		mapper.setContext(type);
		ObjectConverter converter = (ObjectConverter)mapper.getConverter(cls,token.label());
		mapper.consumeLastToken();
		mapper.readUntilLineEnd(in);
		
		Y result = (Y)newInstance(converter.type);
		String newMargin = margin + Mapper.MARGIN_STEP;
		Property property = null;
		for (token = mapper.nextToken(in); !isEnd(token,margin); token = mapper.nextToken(in)) {
			property = converter.getProperties().get(token.label());
			mapper.consumeLastToken();
			Converter<?> propertyConverter = mapper.getConverter(property.isList() ? List.class : property.getType());
			Object value = propertyConverter.fromYaml(in, property.getType(), newMargin);
			try {
				property.setter.invoke(result, value);
			} catch (Exception ex) {
				throw new IOException(ex);
			}
			if (value instanceof Node<?> node) {
				node.setContainer((Node<?>)result);
			} else if (value instanceof List list) {
				for (Object item: list) {
					if (item instanceof Node<?> node) {
						node.setContainer((Node<?>)result);
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

	@Override
	public void writeYaml(Object obj, PrintWriter out, String margin) throws IOException {
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
				} catch (Exception ex) {
					throw new IOException(ex);
				}
				Converter<?> converter = mapper.getConverter(entry.getValue().isList() ? List.class : entry.getValue().getType());
				if (!equalsValue(value,entry.getValue().getDefaultValue())) {
					out.print(margin);
					out.print(entry.getKey());
					out.print(":\t");
					converter.writeYaml(value, out, margin);
				}
			}
			mapper.setContext(oldContext);
		} else {
			throw new UnsupportedOperationException("This Converter only supports objects derived from Node");
		}
	}

}
