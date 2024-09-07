package com.palisand.bones.typedtext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import lombok.Setter;

public class StringConverter implements Converter<String> {
	@Setter private Mapper mapper = null;
	
	@SuppressWarnings("unchecked")
	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		String result = mapper.readUntilLineEnd(in);
		if (result.charAt(result.length()-1) == '\\') {
			StringBuilder sb = new StringBuilder(result);
			do {
				sb.replace(sb.length()-1,sb.length(),"\n");
				result = mapper.readUntilLineEnd(in);
				sb.append(result.substring(margin.length()));
			} while (result.charAt(result.length()-1) == '\\');
			result = sb.toString();
		}
		return (Y)result;
	}

	@Override
	public void writeYaml(Object obj, PrintWriter out, String margin) throws IOException {
		out.println(obj.toString().replace("\n", "\\\n" + margin + Mapper.MARGIN_STEP));
	}

	public Class<?> getType() {
		return String.class;
	}

}
