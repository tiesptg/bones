package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
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
import lombok.ToString;

public class ObjectConverter implements Converter<Object> {
  private static final Map<String, ObjectConverter> CONVERTERS = new TreeMap<>();
  @Getter
  private List<ObjectProperty<?>> properties = new ArrayList<>();
  @Getter
  private Method containerSetter = null;
  @Getter
  private Class<?> type;

  public ObjectProperty<?> getProperty(String name) {
    for (ObjectProperty<?> property : properties) {
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
  @ToString(callSuper = true)
  public static class ObjectProperty<X> extends Property<X> {
    private Class<?> componentType;
    private Class<? extends CustomEditor> editor;

    public ObjectProperty(Class<X> cls, X instance, Field field) throws Exception {
      super(cls, instance, field);
      if (field.getAnnotation(Editor.class) != null) {
        editor = field.getAnnotation(Editor.class).value();
      }
      if (isLink()) {
        setComponentType(Classes.getGenericType(field.getGenericType(), 1));
      } else if (isList()) {
        setComponentType(Classes.getGenericType(field.getGenericType(), 0));
      }
    }

    @Override
    public boolean isValid() {
      return super.isValid() && !isTextIgnore();
    }

    @Override
    public String toString() {
      return "Property of " + getField().toString();
    }

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

    public boolean isList() {
      return List.class.isAssignableFrom(getGetter().getReturnType())
          || LinkList.class.isAssignableFrom(getGetter().getReturnType());
    }

    public boolean isLink() {
      return Link.class.isAssignableFrom(getGetter().getReturnType())
          || LinkList.class.isAssignableFrom(getGetter().getReturnType());
    }

    @Override
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
      if (value == null && getField().getType() == Link.class) {
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
      properties = (List<ObjectProperty<?>>) (Object) (Classes.getProperties(cls,
          (clazz, instance, field) -> new ObjectProperty<X>(clazz, instance, field)));
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }

    properties.sort((p1, p2) -> {
      return compareProperties(cls, p1, p2);
    });
  }

  private FieldOrder getFieldOrder(Class<?> cls) {
    FieldOrder fieldOrder = cls.getAnnotation(FieldOrder.class);
    while (fieldOrder == null && cls.getSuperclass() != Object.class) {
      cls = cls.getSuperclass();
      fieldOrder = cls.getAnnotation(FieldOrder.class);
    }
    return fieldOrder;
  }

  private int compareProperties(Class<?> cls, ObjectProperty<?> p1, ObjectProperty<?> p2) {
    FieldOrder fo = getFieldOrder(cls);
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

  @SuppressWarnings("unchecked")
  private void linkFromTypedText(Parser parser, Object result, ObjectProperty<?> property,
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
    ObjectProperty<?> property = null;
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
            property.set(result, value);
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

  private Object linkToTypedText(Object object, ObjectProperty<?> property, Object value)
      throws IOException {
    Object result = null;
    if (property.isList()) {
      LinkList<?, ?> linkList = (LinkList<?, ?>) value;
      List<String> list = new ArrayList<>();
      result = list;
      List<IOException> exList = new ArrayList<>();
      linkList.getList().forEach(link -> {
        try {
          if (link.isPresent()) {
            list.add(link.getPath());
          }
        } catch (IOException ioex) {
          exList.add(ioex);
        }
      });
      if (!exList.isEmpty()) {
        throw exList.get(0);
      }
    } else {
      Link<?, ?> link = (Link<?, ?>) value;
      if (link.isPresent()) {
        result = link.getPath();
      }
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
      for (ObjectProperty<?> property : properties) {
        if (!property.isTextIgnore()) {
          Object value = null;
          value = property.get(obj);
          if (property.isLink()) {
            value = linkToTypedText(obj, property, value);
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
