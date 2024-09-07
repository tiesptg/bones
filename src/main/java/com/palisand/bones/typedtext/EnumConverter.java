package com.palisand.bones.typedtext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import lombok.Getter;
import lombok.Setter;

public class EnumConverter implements Converter<Enum<?>> {
	@Getter private final Class<?> type;
	@Getter @Setter private Mapper mapper;
	
	public EnumConverter(Class<Enum<?>> type) {
		this.type = type;
	}
	
	public EnumConverter() {
		type = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		String str = mapper.readUntilLineEnd(in);
		if (str.isBlank()) {
			return null;
		}
		for (Object value: type.getEnumConstants()) {
			if (value.toString().equals(str)) {
				return (Y)value;
			}
		}
		return null;
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
