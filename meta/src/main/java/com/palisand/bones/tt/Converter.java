package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import com.palisand.bones.tt.Repository.Parser;
import com.palisand.bones.tt.Repository.Token;

public interface Converter<Y> {
  default void init(Repository repository) {
    
  }
  
	default Y fromTypedText(Parser parser, String str, Class<Y> cls, Class<?> context, String margin) throws IOException {
		return fromTypedText(parser,new BufferedReader(new StringReader(str)),cls, context, margin);
	}
	
	Class<?> getType();
	
	Y fromTypedText(Parser parser, BufferedReader in, Class<?> cls, Class<?> context, String margin) throws IOException;

	default String toTypedText(Repository repository, Y obj, Class<?> context, String margin) throws IOException {
		try (PrintWriter out = new PrintWriter(new StringWriter())) {
			toTypedText(repository,obj,out, context, margin);
			out.flush();
			return out.toString();
		}
	}
	
	void toTypedText(Repository repository, Y obj, PrintWriter out, Class<?> context, String margin) throws IOException;
	
	default Object newInstance(Class<?> cls) {
		try {
			return cls.getConstructor().newInstance();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	default boolean isEnd(Token token, String margin) {
		return token == null || token.isEof() || token.margin().length() < margin.length();
	}
	
	default boolean isValueOnSameLine() {
	  return true;
	}
	

	

}
