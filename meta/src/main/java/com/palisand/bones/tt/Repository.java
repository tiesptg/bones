package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.palisand.bones.log.Logger;
import com.palisand.bones.tt.ObjectConverter.Property;
import lombok.Getter;
import lombok.Setter;

public class Repository {

  private static final Logger LOG = Logger.getLogger(Repository.class);
  private static final Repository INSTANCE = new Repository();
  public static final String MARGIN_STEP = "\t";
  private static final String EXTENSION = ".tt";
  private static final Class<?> ROOT_CONTEXT = Object.class;
  private final Map<Class<?>, Converter<?>> converters = new HashMap<>();
  @Getter
  private final Map<String, SoftReference<Node<?>>> documents = new TreeMap<>();

  public record Token(String margin, String label, char delimiter, int line, int charInLine) {

    public String location() {
      return "Line: " + line + " Char: " + charInLine;
    }

    public boolean isEof() {
      return delimiter == ' ';
    }
  }

  public static Repository getInstance() {
    return INSTANCE;
  }

  @Getter
  @Setter
  public class Parser {
    private Token lastToken;
    private final Set<String> toRead = new TreeSet<>();
    private String reading;
    private int line = 1;
    private int charInLine = 0;

    Repository getRepository() {
      return Repository.this;
    }

    void consumeLastToken() {
      lastToken = null;
    }

    void addToRead(String nodePath) {
      int index = nodePath.indexOf('#');
      if (index > 0) {
        String relativePath = nodePath.substring(0, index);
        String absolutePath = getAbsolutePath(relativePath, reading);
        if (!documents.containsKey(absolutePath)) {
          toRead.add(absolutePath);
        }
      }
    }

    private int read(BufferedReader in) throws IOException {
      ++charInLine;
      return in.read();
    }

    String readUntilLineEnd(BufferedReader in, boolean skipTab) throws IOException {
      if (skipTab) {
        int c = (char) read(in);
        if (c != '\t') {
          throw new IOException("expected tab character but found [" + (char) c + "] at Line: "
              + line + " Char: " + charInLine);
        }
      }
      StringBuilder sb = new StringBuilder();
      int c = (char) read(in);
      // collect chars until end of line
      while (c != -1 && c != '\n' && c != '\r') {
        sb.append((char) c);
        c = read(in);
      }
      // ignore eol
      if (c == '\r') {
        read(in); // skip eol
      }
      ++line;
      charInLine = 0;
      return sb.toString();
    }

    Token nextToken(BufferedReader in) throws IOException {
      if (getLastToken() != null) {
        return getLastToken();
      }
      StringBuilder sb = new StringBuilder();
      String margin = null;
      int labelAtLine = line;
      int labelAtChar = charInLine;
      char c;
      loop: while (true) {
        c = (char) read(in);
        if (c == -1 || c == 0xFFFF) {
          if (sb.isEmpty()) {
            return new Token("", "", ' ', line, charInLine);
          }
          throw new IOException("unexpected end of file reached");
        }
        if (margin == null && c != '\t') {
          margin = sb.toString();
          sb.setLength(0);
          labelAtLine = line;
          labelAtChar = charInLine;
        }
        switch (c) {
          case '>': {
            if (sb.charAt(0) == '\t') {
              sb.deleteCharAt(0);
            }
            break loop;
          }
          case ':':
          case '-':
            break loop;
          default:
            sb.append(c);
            break;
        }
      }
      return lastToken = new Token(margin, sb.toString(), c, labelAtLine, labelAtChar);
    }


  }

  private Repository() {
    addConverter(int.class,
        new PrimitiveConverter<Integer>(int.class, str -> Integer.valueOf(str)));
    addConverter(Integer.class,
        new PrimitiveConverter<Integer>(Integer.class, str -> Integer.valueOf(str)));
    addConverter(double.class,
        new PrimitiveConverter<Double>(double.class, str -> Double.valueOf(str)));
    addConverter(Double.class,
        new PrimitiveConverter<Double>(Double.class, str -> Double.valueOf(str)));
    addConverter(float.class,
        new PrimitiveConverter<Float>(float.class, str -> Float.valueOf(str)));
    addConverter(Float.class,
        new PrimitiveConverter<Float>(Float.class, str -> Float.valueOf(str)));
    addConverter(long.class, new PrimitiveConverter<Long>(long.class, str -> Long.valueOf(str)));
    addConverter(Long.class, new PrimitiveConverter<Long>(Long.class, str -> Long.valueOf(str)));
    addConverter(short.class,
        new PrimitiveConverter<Short>(short.class, str -> Short.valueOf(str)));
    addConverter(Short.class,
        new PrimitiveConverter<Short>(Short.class, str -> Short.valueOf(str)));
    addConverter(byte.class, new PrimitiveConverter<Byte>(byte.class, str -> Byte.valueOf(str)));
    addConverter(Byte.class, new PrimitiveConverter<Byte>(Byte.class, str -> Byte.valueOf(str)));
    addConverter(char.class, new PrimitiveConverter<Character>(char.class, str -> str.charAt(0)));
    addConverter(Character.class,
        new PrimitiveConverter<Character>(Character.class, str -> str.charAt(0)));
    addConverter(boolean.class,
        new PrimitiveConverter<Boolean>(boolean.class, str -> Boolean.valueOf(str)));
    addConverter(Boolean.class,
        new PrimitiveConverter<Boolean>(Boolean.class, str -> Boolean.valueOf(str)));
    addConverter(String.class, new StringConverter());
    addConverter(Object.class, ObjectConverter.getConverter(Object.class));
    addConverter(List.class, new ListConverter());
    addConverter(Enum.class, new EnumConverter());
    addConverter(Link.class, new StringConverter());
    addConverter(LinkList.class, new LinkListConverter());
  }

  Repository addConverter(Class<?> cls, Converter<?> converter) {
    converters.put(cls, converter);
    converter.init(this);
    return this;
  }

  public void clear() {
    documents.clear();
  }

  private String getFullname(Class<?> context, String simpleName) {
    StringBuilder sb = new StringBuilder(context.getName());
    sb.replace(sb.length() - context.getSimpleName().length(), sb.length(), simpleName);
    return sb.toString();
  }

  Converter<?> getConverter(Class<?> context, String name) throws IOException {
    try {
      String fullName = name.contains(".") ? name : getFullname(context, name);
      Class<?> cls = Class.forName(fullName);
      return getConverter(cls);
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  public Converter<?> getConverter(Class<?> cls) {
    if (!List.class.isAssignableFrom(cls)) {
      Converter<?> result = converters.get(cls);
      Class<?> convclass = cls.getSuperclass();
      while (result == null) {
        result = converters.get(convclass);
        convclass = convclass.getSuperclass();
      }
      if (result.getType() != cls && !Modifier.isAbstract(cls.getModifiers())
          && result.getType() != List.class) {
        if (result instanceof ObjectConverter) {
          result = ObjectConverter.getConverter(cls);
          addConverter(cls, result);
        } else if (result instanceof EnumConverter) {
          result = new EnumConverter((Class<Enum<?>>) cls);
          addConverter(cls, result);
        }
      }
      return result;
    }
    return converters.get(List.class);
  }

  @SuppressWarnings("unchecked")
  public <Y> Y fromTypedText(String str) throws IOException {
    return (Y) fromTypedText(new BufferedReader(new StringReader(str)));
  }

  @SuppressWarnings("unchecked")
  public void toTypedText(Object obj, PrintWriter out) throws IOException {
    Converter<Object> converter = (Converter<Object>) getConverter(obj.getClass());
    converter.toTypedText(this, obj, out, ROOT_CONTEXT, "");
  }

  public String toTypedText(Object obj) throws IOException {
    try (StringWriter sw = new StringWriter(); PrintWriter out = new PrintWriter(sw)) {
      toTypedText(obj, out);
      out.flush();
      return sw.toString();
    }
  }

  @SuppressWarnings("unchecked")
  protected <Y> Y fromTypedText(BufferedReader in) throws IOException {
    Converter<?> converter = getConverter(Object.class);
    return (Y) converter.fromTypedText(new Parser(), in, Object.class, ROOT_CONTEXT, "");
  }

  public File write(String absolutePath, Object root) throws IOException {
    File file = new File(absolutePath);
    if (root instanceof Node<?> node) {
      if (!file.exists() && !file.mkdirs()) {
        throw new IOException("Could not create directory " + file.getAbsolutePath());
      }
      if (file.isDirectory()) {
        file = new File(file, node.getId() + EXTENSION);
      } else if (file.isFile() && !file.getName().equals(node.getId() + EXTENSION)) {
        file = new File(file.getParentFile(), node.getId() + EXTENSION);
      }
    } else {
      if (!file.getAbsoluteFile().getParentFile().exists() && !file.getParentFile().mkdirs()) {
        throw new IOException(
            "Could not create parent directory of file " + file.getAbsolutePath());
      }
    }
    if (root instanceof Node<?> node) {
      node.setContainingAttribute(file.getAbsolutePath());
    }

    try (FileWriter fw = new FileWriter(file); PrintWriter out = new PrintWriter(fw)) {
      toTypedText(root, out);
      if (root instanceof Node node) {
        node.setContainingAttribute(file.getAbsolutePath());
        documents.put(file.getAbsolutePath(), new SoftReference<>(node));
      }
    }
    return file;
  }

  private Object readInternal(Parser parser, String absolutePath, Class<?> context)
      throws IOException {
    parser.setReading(absolutePath);
    try (FileReader fr = new FileReader(absolutePath); BufferedReader in = new BufferedReader(fr)) {
      Object root = fromTypedText(in);
      if (root instanceof Node node) {
        node.setContainingAttribute(absolutePath);
        documents.put(absolutePath, new SoftReference<>(node));
      }
      return root;
    }
  }

  public Object read(String absolutePath) throws IOException {
    SoftReference<Node<?>> ref = documents.get(absolutePath);
    Object root = null;
    if (ref != null) {
      root = ref.get();
    }
    if (root == null) {
      Parser parser = new Parser();
      root = readInternal(parser, absolutePath, ROOT_CONTEXT);
      while (!parser.getToRead().isEmpty()) {
        String path = parser.getToRead().iterator().next();
        readInternal(parser, path, ROOT_CONTEXT);
        parser.getToRead().remove(path);
      }
    }
    return root;
  }

  private static String getAbsolutePath(String relativePath, String absoluteContextPath) {
    Path path = Path.of(absoluteContextPath).getParent();
    return path.resolve(relativePath).toString();
  }

  public <N extends Node<?>> N getFromPath(Node<?> context, String path) throws IOException {
    int pos = path.indexOf('#');
    if (pos > 0) {
      String[] parts = path.split("#");
      if (parts.length != 2) {
        throw new IOException("does not contain path before root (#)");
      }

      String absolutePath = getAbsolutePath(parts[0], context.getRootContainer().getFilename());
      Node<?> root = (Node<?>) read(absolutePath);
      String[] pathParts = parts[1].split("/");
      return getFromPath(root, pathParts, 1);
    }
    String[] pathParts = path.split("/");
    return getFromPath(context, pathParts, 0);
  }

  @SuppressWarnings("unchecked")
  private <N extends Node<?>> N getFromPath(Node<?> context, String[] path, int offset) {
    if (offset < path.length) {
      if (path[offset].equals("..")) {
        return getFromPath(context.getContainer(), path, ++offset);
      }
      if (path[offset].equals("#")) {
        return getFromPath((Node<?>) context.getRootContainer(), path, ++offset);
      }
      ObjectConverter converter = (ObjectConverter) getConverter(context.getClass());
      Property property = converter.getProperty(path[offset]);
      if (property == null) {
        System.err.println(
            "property " + path[offset] + " not found in class " + converter.getType() + " path = "
                + Arrays.stream(path).collect(Collectors.joining("/")) + " index = " + offset);
        return null;
      }
      Object value = null;
      try {
        value = property.getGetter().invoke(context);
        final int childOffset = offset + 1;
        if (offset == path.length) {
          return (N) value;
        }
        if (Node.class.isAssignableFrom(property.getComponentType())) {
          if (property.isList()) {
            List<Node<?>> list = (List<Node<?>>) value;
            Optional<Node<?>> node =
                list.stream().filter(item -> path[childOffset].equals(item.getId())).findAny();
            if (node.isPresent()) {
              if (path.length > childOffset + 1) {
                Node<?> result = getFromPath(node.get(), path, childOffset + 1);
                if (result != null) {
                  return (N) result;
                }
              }
              return (N) node.get();
            }
            return null;
          }
          Node<?> node = (Node<?>) value;
          if (path[childOffset].equals(node.getId())) {
            if (path.length > childOffset + 1) {
              Node<?> result = getFromPath(node, path, childOffset + 1);
              if (result != null) {
                return (N) result;
              }
            }
            return (N) node;
          }
          return null;
        }
        throw new UnsupportedOperationException("path in none node list is unsupported");
      } catch (Exception ex) {
        LOG.log("could not get value of " + path[offset] + " in " + this).with(ex).warn();
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <A extends Node<?>> List<A> find(Class<A> type, Node<?> context, String pattern)
      throws IOException {
    List<A> list = new ArrayList<>();
    int index = pattern.indexOf('#');
    if (index == -1) {
      int[] from = {0};
      Node<?> root = findStartNode(context, pattern, from);
      findFromNode(list, type, root, pattern, from[0]);
    } else if (index == 0) {
      assert pattern.charAt(1) == '/';
      if (pattern.equals("#/")) {
        list.add((A) context.getRootContainer());
      } else {
        findFromNode(list, type, (Node<?>) context.getRootContainer(), pattern, 2);
      }
    } else {
      assert pattern.charAt(index + 1) == '/';
      String filePattern = pattern.substring(0, index);
      List<Node<?>> documents = getDocumentsFrom(context.getRootContainer(), filePattern);
      for (Node<?> document : documents) {
        findFromNode(list, type, (Node<?>) document, pattern, index + 2);
      }
    }
    return list;
  }

  public List<Node<?>> getLoadedDocuments() throws IOException {
    List<Node<?>> list = new ArrayList<>();
    for (Entry<String, SoftReference<Node<?>>> entry : documents.entrySet()) {
      Node<?> doc = entry.getValue().get();
      if (doc == null) {
        doc = (Node<?>) read(entry.getKey());
      }
      list.add(doc);
    }
    return list;
  }

  public List<Node<?>> getDocumentsFrom(Node<?> node, String filePattern) throws IOException {
    if (filePattern.equals(".*")) {
      return getLoadedDocuments();
    }
    return getLoadedDocuments().stream()
        .filter(document -> document.getFilename().matches(filePattern)).toList();
  }

  private Node<?> findStartNode(Node<?> node, String pattern, int[] from) {
    while (pattern.indexOf("../", from[0]) == 0) {
      node = node.getContainer();
      from[0] += 3;
    }
    return node;
  }

  @SuppressWarnings("unchecked")
  private <A extends Node<?>> void findFromNode(List<A> result, Class<A> type, Node<?> fromNode,
      String pattern, int startIndex) throws IOException {
    ObjectConverter converter = ObjectConverter.getConverter(fromNode.getClass());
    int endProperty = pattern.indexOf('/', startIndex);
    assert endProperty != -1;
    String name = pattern.substring(startIndex, endProperty);
    Property property = converter.getProperty(name);
    if (property != null && !property.isLink()) {
      if (property.isList()) {
        int idEnd = pattern.indexOf('/', endProperty + 1);
        String idPattern = idEnd == -1 ? pattern.substring(endProperty + 1)
            : pattern.substring(endProperty + 1, idEnd);
        List<A> list = (List<A>) property.get(fromNode);
        if (!idPattern.equals(".*")) {
          list = list.stream().filter(node -> node.getId().matches(idPattern)).toList();
        }
        if (idEnd == -1) {
          if (property.getComponentType() != type) {
            result.addAll(list.stream().filter(a -> type.isAssignableFrom(a.getClass())).toList());
          } else {
            result.addAll(list);
          }
        } else {
          for (Node<?> node : list) {
            findFromNode(result, type, node, pattern, idEnd + 1);
          }
        }
      } else {
        throw new UnsupportedOperationException();
      }
    }
  }

  public void removeRoot(Node<?> node) {
    documents.remove(node.getFilename());
  }

}
