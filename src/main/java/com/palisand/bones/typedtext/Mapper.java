package com.palisand.bones.typedtext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class Mapper {
	
	public static final String MARGIN_STEP = "\t";
	private Map<Class<?>,Converter<?>> converters = new HashMap<>();
	private Token lastToken = null;
	@Getter @Setter private Class<?> context = Node.class;
	
	public record Token(String margin, String label, char delimiter) {}
	
	public Mapper() {
		addConverter(int.class,new PrimitiveConverter(int.class,str -> Integer.valueOf(str)));
		addConverter(Integer.class,new PrimitiveConverter(Integer.class,str -> Integer.valueOf(str)));
		addConverter(double.class,new PrimitiveConverter(double.class,str -> Double.valueOf(str)));
		addConverter(Double.class,new PrimitiveConverter(Double.class,str -> Double.valueOf(str)));
		addConverter(float.class,new PrimitiveConverter(float.class,str -> Float.valueOf(str)));
		addConverter(Float.class,new PrimitiveConverter(Float.class,str -> Float.valueOf(str)));
		addConverter(long.class,new PrimitiveConverter(long.class,str -> Long.valueOf(str)));
		addConverter(Long.class,new PrimitiveConverter(Long.class,str -> Long.valueOf(str)));
		addConverter(short.class,new PrimitiveConverter(short.class,str -> Short.valueOf(str)));
		addConverter(Short.class,new PrimitiveConverter(Short.class,str -> Short.valueOf(str)));
		addConverter(byte.class,new PrimitiveConverter(byte.class,str -> Byte.valueOf(str)));
		addConverter(Byte.class,new PrimitiveConverter(Byte.class,str -> Byte.valueOf(str)));
		addConverter(char.class,new PrimitiveConverter(char.class,str -> str.charAt(0)));
		addConverter(Character.class,new PrimitiveConverter(Character.class,str -> str.charAt(0)));
		addConverter(boolean.class,new PrimitiveConverter(boolean.class,str -> Boolean.valueOf(str)));
		addConverter(Boolean.class,new PrimitiveConverter(Boolean.class,str -> Boolean.valueOf(str)));
		addConverter(String.class,new StringConverter());
		addConverter(Node.class, new ObjectConverter(Node.class));
		addConverter(List.class, new ListConverter());
		addConverter(Enum.class, new EnumConverter());
	}
	
	Mapper addConverter(Class<?> cls, Converter<?> converter) {
		converters.put(cls,converter);
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
			String fullName = name.contains(".") ? name : getFullname(context,name);
			Class<?> cls = Class.forName(fullName);
			return getConverter(cls);
		} catch (ClassNotFoundException ex) {
			throw new IOException(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	Converter<?> getConverter(Class<?> cls) {
		Converter<?> result = converters.get(cls);
		while (result == null) {
			result = converters.get(cls.getSuperclass());
		}
		if (result.getType() != cls && result.getType() != List.class) {
			if (result instanceof ObjectConverter) {
				result = new ObjectConverter(cls);
				addConverter(cls,result);
			} else if (result instanceof EnumConverter) {
				result = new EnumConverter((Class<Enum<?>>)cls);
				addConverter(cls,result);
			}
		}
		return result;
	}
	
	void consumeLastToken() {
		lastToken = null;
	}
	 
	String readUntilLineEnd(BufferedReader in) throws IOException {
		StringBuilder sb = new StringBuilder();
		char c = (char)in.read();
		// collect chars until end of line
		while (c != -1 && c != '\n' && c != '\r') {
			sb.append(c);
			c = (char)in.read();
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
			c = (char)in.read();
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
				in.read();  // skip tab
				break loop;
			}
			case '-' -> {
				in.read(); // skip tab
				break loop;
			}
			default -> sb.append(c);
			}
		}
		return lastToken = new Token(margin,sb.toString(),c);
	}
	
	public <Y> Y fromText(String str, Class<Y> cls) throws IOException {
		return (Y)fromText(new BufferedReader(new StringReader(str)),cls);
	}
	
	public void toText(Object obj, PrintWriter out) throws IOException {
		Converter<?> converter = getConverter(obj.getClass());
		converter.writeYaml(obj, out,"");
	}

	public String toText(Object obj) throws IOException {
		try (StringWriter sw = new StringWriter(); PrintWriter out = new PrintWriter(sw)) {
			toText(obj,out);
			out.flush();
			return sw.toString();
		}
	}
	
	protected <Y> Y fromText(BufferedReader in, Class<Y> cls) throws IOException {
		Converter<?> converter = getConverter(cls);
		return (Y)converter.fromYaml(in,cls,"");
	}

}
