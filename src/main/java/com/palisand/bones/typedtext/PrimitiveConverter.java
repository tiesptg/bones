package com.palisand.bones.typedtext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Function;

import lombok.Getter;
import lombok.Setter;

public class PrimitiveConverter implements Converter<Number> {
	
	private final Function<String,Object> parser;
	@Getter private final Class<?> type;
	@Setter private Mapper mapper = null;
	
	public PrimitiveConverter(Class<?> type, Function<String,Object> parser) {
		this.type = type;
		this.parser = parser;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		String str = mapper.readUntilLineEnd(in);
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
