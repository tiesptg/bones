package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.palisand.bones.tt.Repository.Parser;

import lombok.Getter;
import lombok.Setter;

public class EnumConverter implements Converter<Enum<?>> {
	@Getter private final Class<?> type;
	@Getter @Setter private Repository repository;
	
	public EnumConverter(Class<Enum<?>> type) {
		this.type = type;
	}
	
	public EnumConverter() {
		type = null;
	}

	@Override
	public Enum<?> fromTypedText(Parser parser, BufferedReader in, Class<?> cls, Class<?> context, String margin) throws IOException {
		String str = parser.readUntilLineEnd(in,true);
		if (str.isBlank()) {
			return null;
		}
		for (Object value: type.getEnumConstants()) {
			if (value.toString().equals(str)) {
				return (Enum<?>)value;
			}
		}
		return null;
	}

	@Override
	public void toTypedText(Repository repository, Enum<?> obj, PrintWriter out, Class<?> context, String margin) throws IOException {
		if (obj == null) {
			out.println("null");
		} else {
			out.println(obj.toString());
		}
	}

}
