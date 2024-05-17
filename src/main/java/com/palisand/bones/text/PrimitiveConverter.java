package com.palisand.bones.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Function;

import com.palisand.bones.text.TypedMarkupText.Token;

public class PrimitiveConverter implements Converter<Number> {
	
	private final Function<String,Object> parser;
	private TypedMarkupText container = null;
	
	public void setContainer(TypedMarkupText text) {
		container = text;
	}

	
	public PrimitiveConverter(Function<String,Object> parser) {
		this.parser = parser;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		String str = container.readUntilLineEnd(in);
		if (str.isBlank()) {
			return null;
		}
		return (Y)parser.apply(str);
	}

	@Override
	public void writeYaml(Object obj, PrintWriter out, String margin) throws IOException {
		if (obj == null) {
			out.println("null");
		} else {
			out.println(obj.toString());
		}
	}

}
