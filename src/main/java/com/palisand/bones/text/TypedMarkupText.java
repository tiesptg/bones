package com.palisand.bones.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypedMarkupText {
	
	public static final String MARGIN_STEP = "\t";
	private Map<Class<?>,Converter<?>> converters = new HashMap<>();
	private Token lastToken = null;
	private String packageName;
	
	public record Token(String margin, String label, char delimiter) {}
	
	public TypedMarkupText(String packageName) {
		this.packageName = packageName;
		if (!this.packageName.endsWith(".") && !this.packageName.endsWith("$")) {
			this.packageName += ".";
		}
		addConverter(int.class,new PrimitiveConverter(str -> Integer.valueOf(str)));
		addConverter(Integer.class,new PrimitiveConverter(str -> Integer.valueOf(str)));
		addConverter(double.class,new PrimitiveConverter(str -> Double.valueOf(str)));
		addConverter(Double.class,new PrimitiveConverter(str -> Double.valueOf(str)));
		addConverter(float.class,new PrimitiveConverter(str -> Float.valueOf(str)));
		addConverter(Float.class,new PrimitiveConverter(str -> Float.valueOf(str)));
		addConverter(long.class,new PrimitiveConverter(str -> Long.valueOf(str)));
		addConverter(Long.class,new PrimitiveConverter(str -> Long.valueOf(str)));
		addConverter(short.class,new PrimitiveConverter(str -> Short.valueOf(str)));
		addConverter(Short.class,new PrimitiveConverter(str -> Short.valueOf(str)));
		addConverter(byte.class,new PrimitiveConverter(str -> Byte.valueOf(str)));
		addConverter(Byte.class,new PrimitiveConverter(str -> Byte.valueOf(str)));
		addConverter(char.class,new PrimitiveConverter(str -> str.charAt(0)));
		addConverter(Character.class,new PrimitiveConverter(str -> str.charAt(0)));
		addConverter(boolean.class,new PrimitiveConverter(str -> Boolean.valueOf(str)));
		addConverter(Boolean.class,new PrimitiveConverter(str -> Boolean.valueOf(str)));
		addConverter(String.class,new StringConverter());
		addConverter(Object.class, new ObjectConverter(Object.class));
		addConverter(List.class, new ListConverter());
	}
	
	TypedMarkupText addConverter(Class<?> cls, Converter<?> converter) {
		converters.put(cls,converter);
		converter.setContainer(this);
		return this;
	}
	
	Converter<?> getConverter(String name) throws IOException {
		try {
			Class<?> cls = Class.forName(packageName + name);
			return getConverter(cls);
		} catch (ClassNotFoundException ex) {
			throw new IOException(ex);
		}
	}
	
	Converter<?> getConverter(Class<?> cls) {
		Converter<?> result = converters.get(cls);
		while (result == null) {
			result = converters.get(cls.getSuperclass());
		}
		if (result instanceof ObjectConverter oc && oc.getType() != cls) {
			result = new ObjectConverter(cls);
			addConverter(cls,result);
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
	
	public <Y> Y fromYaml(String str, Class<Y> cls) throws IOException {
		return (Y)fromYaml(new BufferedReader(new StringReader(str)),cls);
	}
	
	public void writeYaml(Object obj, PrintWriter out) throws IOException {
		Converter<?> converter = getConverter(obj.getClass());
		converter.writeYaml(obj, out,"");
	}

	public String toYaml(Object obj) throws IOException {
		try (StringWriter sw = new StringWriter(); PrintWriter out = new PrintWriter(sw)) {
			writeYaml(obj,out);
			out.flush();
			return sw.toString();
		}
	}
	
	protected <Y> Y fromYaml(BufferedReader in, Class<Y> cls) throws IOException {
		Converter<?> converter = getConverter(cls);
		return (Y)converter.fromYaml(in,cls,"");
	}

}
