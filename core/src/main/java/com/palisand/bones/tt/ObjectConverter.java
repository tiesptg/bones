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

import com.palisand.bones.tt.Mapper.Token;

import java.util.TreeMap;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObjectConverter implements Converter<Object> {
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
		private String refPattern = null;
		private String refOpposite = null;
		
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
		while (cls != Node.class && cls != Object.class) {
			for (Method method: cls.getDeclaredMethods()) {
				String name = isGetter(method);
				if (name != null) {
					Property property = getProperty(name);
					property.setGetter(method);
					try {
						Field field = cls.getDeclaredField(name);
						Ref ref = field.getAnnotation(Ref.class);
						if (ref != null) {
							property.setRefPattern(ref.value());
						}
					} catch (NoSuchFieldException ex) {
						// ignore
					}
					try {
						Object object = newInstance(cls);
						property.setDefaultValue(method.invoke(object));
						if (List.class.isAssignableFrom(property.getType())) {
							property.setComponentType(getListType(property.getGetter().getGenericReturnType()));
						}
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
		// remove incomplete properties
		List<String> toRemove = properties.entrySet().stream()
				.filter(e -> e.getValue().getSetter() == null || e.getValue().getGetter() == null)
				.map(e -> e.getKey()).toList();
		toRemove.forEach(name -> properties.remove(name));
	}
	
	@SuppressWarnings("unchecked")
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
		String newMargin = margin + Mapper.MARGIN_STEP;
		Property property = null;
		for (token = mapper.nextToken(in); !isEnd(token,margin); token = mapper.nextToken(in)) {
			property = converter.getProperties().get(token.label());
			mapper.consumeLastToken();
			if (property.refPattern != null && property.getType() != ExternalLink.class && result instanceof Node<?> node) {
				Converter<String> strconv = (Converter<String>)mapper.getConverter(String.class);
				String reference = strconv.fromYaml(in, String.class, newMargin);
				mapper.addReference(node,property,reference);
			} else {
				Converter<?> propertyConverter = mapper.getConverter(property.isList() ? List.class : property.getType());
				Object value = propertyConverter.fromYaml(in, property.getType(), newMargin);
				try {
					property.setter.invoke(result, value);
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
				if (entry.getValue().refPattern != null) {
					if (value != null) {
						if (value instanceof ExternalLink link) {
							if (link.getPath() != null) {
								out.print(margin);
								out.print(entry.getKey());
								out.print(":\t");
								StringConverter converter = (StringConverter)mapper.getConverter(String.class);
								converter.toYaml(link.getPath(), out, margin);
							}
						} else {
							boolean isRelative = !entry.getValue().refPattern.contains("#");
							out.print(margin);
							out.print(entry.getKey());
							out.print(":\t");
							StringConverter converter = (StringConverter)mapper.getConverter(String.class);
							Node<?> child = (Node<?>)value;
							if (isRelative) {
								converter.toYaml(child.getRelativePath(node), out, margin);
							} else {
								converter.toYaml(child.getAbsolutePath(), out, margin);
							}
						}
					}
				} else {
					Converter<Object> converter = (Converter<Object>)mapper.getConverter(entry.getValue().isList() ? List.class : entry.getValue().getType());
					if (!equalsValue(value,entry.getValue().getDefaultValue())) {
						out.print(margin);
						out.print(entry.getKey());
						out.print(":\t");
						converter.toYaml(value, out, margin);
					}
				}
			}
			mapper.setContext(oldContext);
		} else {
			throw new UnsupportedOperationException("This Converter only supports objects derived from Node");
		}
	}

}
