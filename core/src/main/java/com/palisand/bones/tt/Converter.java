package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import com.palisand.bones.tt.Repository.Token;

public interface Converter<Y> {
	default Y fromTypedText(String str, Class<Y> cls, String margin) throws IOException {
		return fromTypedText(new BufferedReader(new StringReader(str)),cls,margin);
	}
	
	Class<?> getType();
	
	Y fromTypedText(BufferedReader in, Class<?> cls, String margin) throws IOException;

	default String toTypedText(Y obj, String margin) throws IOException {
		try (PrintWriter out = new PrintWriter(new StringWriter())) {
			toTypedText(obj,out, margin);
			out.flush();
			return out.toString();
		}
	}
	
	void toTypedText(Y obj, PrintWriter out, String margin) throws IOException;
	
	void setRepository(Repository text);
	
	default Object newInstance(Class<?> cls) {
		try {
			return cls.getConstructor().newInstance();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	default boolean isEnd(Token token, String margin) {
		return token == null || !token.margin().equals(margin);
	}
	

	

}
