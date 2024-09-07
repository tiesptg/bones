package com.palisand.bones.typedtext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import com.palisand.bones.typedtext.Mapper.Token;

public interface Converter<T> {
	default <Y> Y fromYaml(String str, Class<Y> cls, String margin) throws IOException {
		return fromYaml(new BufferedReader(new StringReader(str)),cls,margin);
	}
	
	Class<?> getType();
	
	<Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException;

	default String toYaml(Object obj, String margin) throws IOException {
		try (PrintWriter out = new PrintWriter(new StringWriter())) {
			writeYaml(obj,out, margin);
			out.flush();
			return out.toString();
		}
	}
	
	void writeYaml(Object obj, PrintWriter out, String margin) throws IOException;
	
	void setMapper(Mapper text);
	
	default Object newInstance(Class<?> cls) {
		try {
			return cls.getConstructor().newInstance();
		} catch (Exception ex) {
			throw new UnsupportedOperationException(cls + " does not have a public default constructor",ex);
		}
	}
	
	default boolean isEnd(Token token, String margin) {
		return token == null || !token.margin().equals(margin);
	}
	

	

}
