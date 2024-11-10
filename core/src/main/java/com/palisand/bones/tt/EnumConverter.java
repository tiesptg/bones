package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

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
	public Enum<?> fromTypedText(BufferedReader in, Class<?> cls, String margin) throws IOException {
		String str = repository.readUntilLineEnd(in);
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
	public void toTypedText(Enum<?> obj, PrintWriter out, String margin) throws IOException {
		if (obj == null) {
			out.println("null");
		} else {
			out.println(obj.toString());
		}
	}

}
