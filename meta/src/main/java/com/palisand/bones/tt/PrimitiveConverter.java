package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Function;

import com.palisand.bones.tt.Repository.Parser;

import lombok.Getter;
import lombok.Setter;

public class PrimitiveConverter<Y> implements Converter<Y> {
	
	private final Function<String,Object> converter;
	@Getter private final Class<?> type;
	@Setter private Repository repository = null;
	
	public PrimitiveConverter(Class<?> type, Function<String,Object> converter) {
		this.type = type;
		this.converter = converter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Y fromTypedText(Parser parser, BufferedReader in, Class<?> cls, Class<?> context, String margin) throws IOException {
		String str = parser.readUntilLineEnd(in);
		if (str.isBlank()) {
			return null;
		}
		return (Y)converter.apply(str);
	}

	@Override
	public void toTypedText(Repository repository, Y obj, PrintWriter out, Class<?> context, String margin) throws IOException {
		if (obj == null) {
			out.println("null");
		} else {
			out.println(obj.toString());
		}
	}

}
