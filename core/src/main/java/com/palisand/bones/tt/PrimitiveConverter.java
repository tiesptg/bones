package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Function;

import lombok.Getter;
import lombok.Setter;

public class PrimitiveConverter<Y> implements Converter<Y> {
	
	private final Function<String,Object> parser;
	@Getter private final Class<?> type;
	@Setter private Repository repository = null;
	
	public PrimitiveConverter(Class<?> type, Function<String,Object> parser) {
		this.type = type;
		this.parser = parser;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Y fromTypedText(BufferedReader in, Class<?> cls, String margin) throws IOException {
		String str = repository.readUntilLineEnd(in);
		if (str.isBlank()) {
			return null;
		}
		return (Y)parser.apply(str);
	}

	@Override
	public void toTypedText(Y obj, PrintWriter out, String margin) throws IOException {
		if (obj == null) {
			out.println("null");
		} else {
			out.println(obj.toString());
		}
	}

}
