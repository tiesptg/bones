package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.palisand.bones.di.Classes;
import com.palisand.bones.tt.Repository.Parser;
import com.palisand.bones.tt.Repository.Token;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObjectConverter implements Converter<Object> {
	private static final Map<String,ObjectConverter> CONVERTERS = new TreeMap<>();
	@Getter private List<Property> properties = new ArrayList<>();
	@Getter private Method containerSetter = null;
	@Getter private Class<?> type;
	
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
		private Rules rules;
		private Class<? extends CustomEditor> editor;
		
		public Class<?> getType() {
			return getter.getReturnType();
		}
		
		public Class<?> getComponentType() {
			if (componentType != null) {
				return componentType;
			}
			return getType();
		}
		
		public Object get(Object target) throws IOException {
		  return invoke(getter,target);
		}
		
		public void set(Object target, Object value) throws IOException {
		  invoke(setter,target,value);
		}
		
	  private Object invoke(Method method, Object target, Object...parameters) throws IOException {
	    try {
	      return method.invoke(target,parameters);
      } catch (IllegalAccessException e) {
        throw new IOException(e);
      } catch (IllegalArgumentException e) {
        assert false;
        throw new IOException(e);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException)e.getCause();
        } else if (e.getCause() instanceof IOException) {
          throw (IOException)e.getCause();
        }
        throw new IOException(e.getCause());
	    }
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
	
	private boolean isPropertyGetter(Method method, StringBuilder name) {
		if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0 && method.getAnnotation(TextIgnore.class) == null) {
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
			if (isPropertyGetter(method,name) && method.getDeclaringClass() != Object.class && method.getDeclaringClass() != Node.class) {
				name.insert(0, "set");
				Property property = new Property();
				property.setGetter(method);
				if (method.getAnnotation(Editor.class) != null) {
				  Editor editor = method.getAnnotation(Editor.class);
				  property.setEditor(editor.value());
				}
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
	private void linkFromTypedText(Parser parser, Object result, Property property, Object value) throws Exception {
		if (property.isList()) {
			List<String> list = (List<String>)value;
			LinkList<?,?> linkList = (LinkList<?,?>)property.getGetter().invoke(result);
			for (String path: list) {
				linkList.addPath(path);
				parser.addToRead(path);
			}
		} else {
			Link<?,?> link = (Link<?,?>)property.getter.invoke(result);
			link.setPath((String)value);
      parser.addToRead(link.getPath());
		}
	}
	
	private void setContainer(Object value, Object parent, String containerProperty) {
    if (value instanceof Node<?> node) {
      node.setContainer((Node<?>)parent,containerProperty);
    } else if (value instanceof List list) {
      for (Object item: list) {
        if (item instanceof Node<?> node) {
          node.setContainer((Node<?>)parent,containerProperty);
        }
      }
    }
	}
	
	@Override
	public Object fromTypedText(Parser parser, BufferedReader in, Class<?> cls, Class<?> context, String margin) throws IOException {
		Token token = parser.nextToken(in);
		assert token.delimiter() == '>';
		Repository repository = parser.getRepository();
		ObjectConverter converter = (ObjectConverter)repository.getConverter(cls,token.label());
		parser.consumeLastToken();
		parser.readUntilLineEnd(in);
		
		Object result = newInstance(converter.type);
		String newMargin = margin + Repository.MARGIN_STEP;
		Property property = null;
		for (token = parser.nextToken(in); !isEnd(token,margin); token = parser.nextToken(in)) {
			property = converter.getProperty(token.label());
			parser.consumeLastToken();
			if (property != null && !property.isReadonly()) {
				Converter<?> propertyConverter = repository.getConverter(property.getType());
				Object value = propertyConverter.fromTypedText(parser,in, property.getComponentType(), cls,newMargin);
        if (!property.isLink()) {
          setContainer(value,result,token.label());
        }
				try {
					if (property.isLink()) {
						linkFromTypedText(parser,result,property,value);
					} else {
						property.setter.invoke(result, value);
					}
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			} else {
				Converter<?> strConv = repository.getConverter(String.class);
				strConv.fromTypedText(parser,in, String.class, cls, newMargin);
				for (token = parser.nextToken(in); isEnd(token,margin); token = parser.nextToken(in)) {
					parser.consumeLastToken();
					strConv.fromTypedText(parser, in, String.class, cls, newMargin);
				}
			}
		}
		return result;
	}
	
	private String getClassLabel(Class<?> cls, Class<?> context) {
		Package pack = context.getPackage();
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
		} else {
			Link<?,?> link = (Link<?,?>)value;
			result = link.getPath();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void toTypedText(Repository repository, Object obj, PrintWriter out, Class<?> context, String margin) throws IOException {
		if (obj == null) {
			out.println("null");
		} else {
			out.print(getClassLabel(obj.getClass(),context));
			out.println('>');
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
							converter.toTypedText(repository,value, out, obj.getClass(), newMargin);
						} else {
							out.println("null");
						}
					}
				}
			}
		}
	}
	
}
