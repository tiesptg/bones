package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.palisand.bones.Classes;
import com.palisand.bones.Classes.Property;
import com.palisand.bones.meta.ui.CustomEditor;
import com.palisand.bones.tt.Repository.Parser;
import com.palisand.bones.tt.Repository.Token;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObjectConverter implements Converter<Object> {
  private static final Map<String, ObjectConverter> CONVERTERS = new TreeMap<>();
  @Getter private List<EditorProperty<?>> properties = new ArrayList<>();
  @Getter private Method containerSetter = null;
  @Getter private Class<?> type;

  public EditorProperty<?> getProperty(String name) {
    for (EditorProperty<?> property : properties) {
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
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class EditorProperty<X> extends Property<X> {
    private Class<?> componentType;
    private Class<? extends CustomEditor> editor;

    public String getName() {
      return getField().getName();
    }

    public Class<?> getType() {
      return getField().getType();
    }

    public Class<?> getComponentType() {
      if (componentType != null) {
        return componentType;
      }
      return getType();
    }

    public Object getValue(Object target) throws IOException {
      return invoke(getGetter(), target);
    }

    public void setValue(Object target, Object value) throws IOException {
      invoke(getSetter(), target, value);
    }

    private Object invoke(Method method, Object target, Object... parameters) throws IOException {
      try {
        return method.invoke(target, parameters);
      } catch (IllegalAccessException e) {
        throw new IOException(e);
      } catch (IllegalArgumentException e) {
        assert false;
        throw new IOException(e);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        } else if (e.getCause() instanceof IOException) {
          throw (IOException) e.getCause();
        }
        throw new IOException(e.getCause());
      }
    }

    public boolean isList() {
      return List.class.isAssignableFrom(getGetter().getReturnType())
          || LinkList.class.isAssignableFrom(getGetter().getReturnType());
    }

    public boolean isLink() {
      return Link.class.isAssignableFrom(getGetter().getReturnType())
          || LinkList.class.isAssignableFrom(getGetter().getReturnType());
    }

    public boolean isReadonly() {
      return getSetter() == null && !isLink();
    }

    public boolean isTextIgnore() {
      return getField().getAnnotation(TextIgnore.class) != null || isReadonly();
    }

    public boolean hasTextIgnoreAnnotation() {
      return getField().getAnnotation(TextIgnore.class) != null;
    }

    public String getLabel() {
      return Character.toUpperCase(getName().charAt(0)) + getName().substring(1);
    }

    public boolean isDefault(Object value) {
      if (value == null && getDefaultValue() == null) {
        return true;
      }
      if (value instanceof LinkList linkList) {
        return linkList.isEmpty();
      }
      if (value instanceof List list) {
        return list.isEmpty();
      }
      if (value == null || getDefaultValue() == null) {
        return false;
      }
      if (value.equals(getDefaultValue())) {
        return true;
      }
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private <X> ObjectConverter(Class<X> cls) {
    type = cls;
    try {
      properties = (List<EditorProperty<?>>) (Object) (Classes.getProperties(cls,
          () -> new EditorProperty<X>(), property -> {
            if (property.getField().getAnnotation(Editor.class) != null) {
              Editor editor = property.getField().getAnnotation(Editor.class);
              property.setEditor(editor.value());
            }
            if (property.isLink()) {
              property.setComponentType(
                  Classes.getGenericType(property.getField().getGenericType(), 1));
            } else if (property.isList()) {
              property.setComponentType(
                  Classes.getGenericType(property.getField().getGenericType(), 0));
            }
          }));
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }

    properties.sort((p1, p2) -> {
      int rt =
          compareClasses(p1.getGetter().getDeclaringClass(), p2.getGetter().getDeclaringClass());
      if (rt == 0) {
        rt = compareProperties(p1, p2);
      }
      return rt;
    });
  }

  private int compareProperties(EditorProperty<?> p1, EditorProperty<?> p2) {
    FieldOrder fo = p1.getGetter().getDeclaringClass().getAnnotation(FieldOrder.class);
    if (fo != null) {
      int i1 = indexOf(fo.value(), p1.getName());
      int i2 = indexOf(fo.value(), p2.getName());
      if (i1 < 1000 || i2 < 1000) {
        return i1 - i2;
      }
    }
    return p1.getName().compareTo(p2.getName());
  }

  private int indexOf(String[] array, String value) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i].equals(value)) {
        return i;
      }
    }
    return 1000;
  }

  private int compareClasses(Class<?> c1, Class<?> c2) {
    if (c1 == c2) {
      return 0;
    }
    if (c1.isInterface() && !c2.isInterface()) {
      return 1;
    }
    if (c2.isInterface() && !c1.isInterface()) {
      return -1;
    }
    if (c1.isAssignableFrom(c2)) {
      return -1;
    }
    return 1; // c2.isAssignableFrom(c1);
  }

  @SuppressWarnings("unchecked")
  private void linkFromTypedText(Parser parser, Object result, EditorProperty<?> property,
      Object value) throws Exception {
    if (property.isList()) {
      List<String> list = (List<String>) value;
      LinkList<?, ?> linkList = (LinkList<?, ?>) property.getGetter().invoke(result);
      for (String path : list) {
        linkList.addPath(path);
        parser.addToRead(path);
      }
    } else {
      Link<?, ?> link = (Link<?, ?>) property.get(result);
      link.setPath((String) value);
      parser.addToRead(link.getPath());
    }
  }

  private void setContainer(Object value, Object parent, String containerProperty) {
    if (value instanceof Node<?> node) {
      node.setContainer((Node<?>) parent, containerProperty);
    } else if (value instanceof List list) {
      for (Object item : list) {
        if (item instanceof Node<?> node) {
          node.setContainer((Node<?>) parent, containerProperty);
        }
      }
    }
  }

  @Override
  public Object fromTypedText(Parser parser, BufferedReader in, Class<?> cls, Class<?> context,
      String margin) throws IOException {
    Token token = parser.nextToken(in);
    if (token.delimiter() != '>') {
      throw new IOException("Expected type at " + token.location() + " but found: [" + token.label()
          + "] '>' is missing");
    }
    Repository repository = parser.getRepository();
    ObjectConverter converter = (ObjectConverter) repository.getConverter(cls, token.label());
    parser.consumeLastToken();
    parser.readUntilLineEnd(in, false);

    Object result = newInstance(converter.type);
    String newMargin = margin + Repository.MARGIN_STEP;
    EditorProperty<?> property = null;
    for (token = parser.nextToken(in); !isEnd(token, margin); token = parser.nextToken(in)) {
      property = converter.getProperty(token.label());
      parser.consumeLastToken();
      if (property != null && !property.isReadonly()) {
        Converter<?> propertyConverter = repository.getConverter(property.getType());
        Object value = propertyConverter.fromTypedText(parser, in, property.getComponentType(), cls,
            newMargin);
        if (!property.isLink()) {
          setContainer(value, result, token.label());
        }
        try {
          if (property.isLink()) {
            linkFromTypedText(parser, result, property, value);
          } else {
            property.setValue(result, value);
          }
        } catch (Exception ex) {
          if (ex instanceof IOException ioe) {
            throw ioe;
          }
          throw new IOException(ex);
        }
      } else {
        // skip and ignore unknown property
        parser.readUntilLineEnd(in, false);
        for (token = parser.nextToken(in); canIgnore(token, margin); token = parser.nextToken(in)) {
          parser.consumeLastToken();
          parser.readUntilLineEnd(in, false);
        }
      }
    }
    return result;
  }

  private boolean canIgnore(Token token, String margin) {
    return token != null && !token.isEof() && token.margin().length() > margin.length();
  }

  private String getClassLabel(Class<?> cls, Class<?> context) {
    Package pack = context.getPackage();
    if (pack.equals(cls.getPackage())) {
      return cls.getSimpleName();
    }
    return cls.getName();
  }

  private Object linkToTypedText(Object object, EditorProperty<?> property, Object value)
      throws IOException {
    Object result = null;
    if (property.isList()) {
      LinkList<?, ?> linkList = (LinkList<?, ?>) value;
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
      Link<?, ?> link = (Link<?, ?>) value;
      result = link.getPath();
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void toTypedText(Repository repository, Object obj, PrintWriter out, Class<?> context,
      String margin) throws IOException {
    if (obj == null) {
      out.println("null");
    } else {
      out.print(getClassLabel(obj.getClass(), context));
      out.println('>');
      for (EditorProperty<?> property : properties) {
        if (!property.isTextIgnore() && !property.isReadonly()) {
          Object value = null;
          try {
            value = property.getGetter().invoke(obj);
            if (property.isLink()) {
              value = linkToTypedText(obj, property, value);
            }
          } catch (Exception ex) {
            throw new IOException(ex);
          }
          String newMargin = margin + Repository.MARGIN_STEP;
          Converter<Object> converter =
              value == null ? (Converter<Object>) repository.getConverter(property.getType())
                  : (Converter<Object>) repository.getConverter(value.getClass());
          if (!property.isDefault(value)) {
            out.print(margin);
            out.print(property.getName());
            out.print(':');
            if (converter != null) {
              if (converter.isValueOnSameLine()) {
                out.print('\t');
              }
              converter.toTypedText(repository, value, out, obj.getClass(), newMargin);
            } else {
              out.println("\tnull");
            }
          }
        }
      }
    }
  }

  @Override
  public boolean isValueOnSameLine() {
    return false;
  }
}
