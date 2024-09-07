package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.palisand.bones.tt.ObjectConverter.Property;

import lombok.Getter;
import lombok.Setter;

public class Repository {

	private static final Logger LOG = LogManager.getLogger(Repository.class);
	public static final String MARGIN_STEP = "\t";
	private final Map<Class<?>, Converter<?>> converters = new HashMap<>();
	private final Map<String,Node<?>> documents = new TreeMap<>();
	private Token lastToken = null;
	
	@Getter @Setter private Class<?> context = Node.class;

	public record Token(String margin, String label, char delimiter) {
	}

	public Repository() {
		addConverter(int.class, new PrimitiveConverter<Integer>(int.class, str -> Integer.valueOf(str)));
		addConverter(Integer.class, new PrimitiveConverter<Integer>(Integer.class, str -> Integer.valueOf(str)));
		addConverter(double.class, new PrimitiveConverter<Double>(double.class, str -> Double.valueOf(str)));
		addConverter(Double.class, new PrimitiveConverter<Double>(Double.class, str -> Double.valueOf(str)));
		addConverter(float.class, new PrimitiveConverter<Float>(float.class, str -> Float.valueOf(str)));
		addConverter(Float.class, new PrimitiveConverter<Float>(Float.class, str -> Float.valueOf(str)));
		addConverter(long.class, new PrimitiveConverter<Long>(long.class, str -> Long.valueOf(str)));
		addConverter(Long.class, new PrimitiveConverter<Long>(Long.class, str -> Long.valueOf(str)));
		addConverter(short.class, new PrimitiveConverter<Short>(short.class, str -> Short.valueOf(str)));
		addConverter(Short.class, new PrimitiveConverter<Short>(Short.class, str -> Short.valueOf(str)));
		addConverter(byte.class, new PrimitiveConverter<Byte>(byte.class, str -> Byte.valueOf(str)));
		addConverter(Byte.class, new PrimitiveConverter<Byte>(Byte.class, str -> Byte.valueOf(str)));
		addConverter(char.class, new PrimitiveConverter<Character>(char.class, str -> str.charAt(0)));
		addConverter(Character.class, new PrimitiveConverter<Character>(Character.class, str -> str.charAt(0)));
		addConverter(boolean.class, new PrimitiveConverter<Boolean>(boolean.class, str -> Boolean.valueOf(str)));
		addConverter(Boolean.class, new PrimitiveConverter<Boolean>(Boolean.class, str -> Boolean.valueOf(str)));
		addConverter(String.class, new StringConverter());
		addConverter(Object.class, new ObjectConverter(Object.class));
		addConverter(List.class, new ListConverter());
		addConverter(Enum.class, new EnumConverter());
		addConverter(Link.class, new StringConverter());
	}

	Repository addConverter(Class<?> cls, Converter<?> converter) {
		converters.put(cls, converter);
		converter.setMapper(this);
		return this;
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
		Converter<?> result = converters.get(cls);
		Class<?> convclass = cls.getSuperclass();
		while (result == null) {
			result = converters.get(convclass);
			convclass = convclass.getSuperclass();
		}
		if (result.getType() != cls && result.getType() != List.class) {
			if (result instanceof ObjectConverter) {
				result = new ObjectConverter(cls);
				addConverter(cls, result);
			} else if (result instanceof EnumConverter) {
				result = new EnumConverter((Class<Enum<?>>) cls);
				addConverter(cls, result);
			}
		}
		return result;
	}

	void consumeLastToken() {
		lastToken = null;
	}

	String readUntilLineEnd(BufferedReader in) throws IOException {
		StringBuilder sb = new StringBuilder();
		char c = (char) in.read();
		// collect chars until end of line
		while (c != -1 && c != '\n' && c != '\r') {
			sb.append(c);
			c = (char) in.read();
		}
		// ignore eol
		if (c == '\r') {
			in.read(); // skip eol
		}
		return sb.toString();
	}

	Token nextToken(BufferedReader in) throws IOException {
		if (lastToken != null) {
			return lastToken;
		}
		StringBuilder sb = new StringBuilder();
		String margin = null;
		char c;
		loop: while (true) {
			c = (char) in.read();
			if (c == -1 || c == 0xFFFF) {
				if (sb.isEmpty()) {
					return null;
				}
				throw new IOException("unexpected end of file reached");
			}
			if (margin == null && !Character.isWhitespace(c)) {
				margin = sb.toString();
				sb.setLength(0);
			}
			switch (c) {
			case '>' -> {
				break loop;
			}
			case ':' -> {
				in.read(); // skip tab
				break loop;
			}
			case '-' -> {
				in.read(); // skip tab
				break loop;
			}
			default -> sb.append(c);
			}
		}
		return lastToken = new Token(margin, sb.toString(), c);
	}

	@SuppressWarnings("unchecked")
	public <Y> Y fromText(String str) throws IOException {
		return (Y) fromText(new BufferedReader(new StringReader(str)));
	}

	@SuppressWarnings("unchecked")
	public void toText(Object obj, PrintWriter out) throws IOException {
		Converter<Object> converter = (Converter<Object>)getConverter(obj.getClass());
		converter.toYaml(obj, out, "");
	}

	public String toText(Object obj) throws IOException {
		try (StringWriter sw = new StringWriter(); PrintWriter out = new PrintWriter(sw)) {
			toText(obj, out);
			out.flush();
			return sw.toString();
		}
	}

	@SuppressWarnings("unchecked")
	protected <Y> Y fromText(BufferedReader in) throws IOException {
		Converter<?> converter = getConverter(Object.class);
		return (Y) converter.fromYaml(in, Object.class, "");
	}
	
	public File write(String absolutePath, Object root) throws IOException {
		File file = new File(absolutePath);
		if (root instanceof Node<?> node) {
			if (file.exists()) {
				if (!file.isDirectory()) {
					throw new IOException(absolutePath + " should be the directory where the file is created, but it exists and is not a directory");
				}
			} else if (!file.mkdirs()) {
				throw new IOException("Could not create directory " + file.getAbsolutePath());
			}
			file = new File(file,node.getId() + ".tt");
		} else {
			if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
				throw new IOException("Could not create parent directory of file " + file.getAbsolutePath());
			}
		}
		if (root instanceof Node<?> node) {
			node.setContainingAttribute(file.getAbsolutePath());
		}

		try (FileWriter fw = new FileWriter(file);
			PrintWriter out = new PrintWriter(fw)) {
			toText(root,out);
			if (root instanceof Node<?> node) {
				node.setContainingAttribute(file.getAbsolutePath());
				documents.put(file.getAbsolutePath(), node);
			}
		}
		return file;
	}
	
	public Object read(String absolutePath) throws IOException {
		Object root = documents.get(absolutePath);
		if (root == null) {
			try (FileReader fr = new FileReader(absolutePath);
				BufferedReader in = new BufferedReader(fr)) {
				root = fromText(in);
				if (root instanceof Node<?> node) {
					node.setContainingAttribute(absolutePath);
					documents.put(absolutePath, node);
				}
			}
		}
		return root;
	}
	
	private static String getAbsolutePath(String relativePath, String absoluteContextPath) {
		Path path = Path.of(relativePath);
		return path.resolve(absoluteContextPath).toString();
	}

	public <N extends Node<?>> N getFromPath(Node<?> context, String path) throws IOException {
		int pos = path.indexOf('#');
		if (pos > 0) {
			String[] parts = path.split("#");
			if (parts.length != 2) {
				throw new IOException("does not contain path before root (#)");
			}
			
			String absolutePath = getAbsolutePath(parts[0], context.getRootContainer().getContainingAttribute());
			Node<?> root = (Node<?>)read(absolutePath);
			String[] pathParts = parts[1].split("/");
			return getFromPath(root,pathParts,0);
		}
		String[] pathParts = path.split("/");
		return getFromPath(context,pathParts,0);
	}
	
	@SuppressWarnings("unchecked")
	private <N extends Node<?>> N getFromPath(Node<?> context, String[] path, int offset) {
		if (offset < path.length) {
			if (path[offset].equals("..")) {
				return getFromPath(context.getContainer(),path, ++offset);
			}
			if (path[offset].equals("#")) {
				return getFromPath(context.getRootContainer(),path,++offset);
			}
			ObjectConverter converter = (ObjectConverter)getConverter(getClass());
			Property property = converter.getProperties().get(path[offset]);
			Object value = null;
			try {
				value = property.getGetter().invoke(this);
				final int childOffset = offset + 1;
				if (offset == path.length) {
					return (N)value;
				}
				if (Node.class.isAssignableFrom(property.getComponentType())) {
					if (property.isList()) {
						List<Node<?>> list = (List<Node<?>>)value;
						Optional<Node<?>> node = list.stream().filter(item -> path[childOffset].equals(item.getId())).findAny();
						if (node.isPresent()) {
							return (N)node.get();
						}
						return null;
					}
					Node<?> node = (Node<?>)value;
					if (path[childOffset].equals(node.getId())) {
						return (N)node;
					}
					return null;
				}
				throw new UnsupportedOperationException("path in none node list is unsupported");
			} catch (Exception ex) {
				LOG.info("could not get value of " + path[offset] + " in " + this,ex);
			}
		}
		return null;
	}
	


}
