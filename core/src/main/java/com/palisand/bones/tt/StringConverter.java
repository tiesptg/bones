package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import lombok.Setter;

public class StringConverter implements Converter<String> {
	@Setter private Repository mapper = null;
	
	@Override
	public String fromYaml(BufferedReader in, Class<?> cls, String margin) throws IOException {
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
		return result;
	}

	@Override
	public void toYaml(String str, PrintWriter out, String margin) throws IOException {
		out.println(str.replace("\n", "\\\n" + margin + Repository.MARGIN_STEP));
	}

	public Class<?> getType() {
		return String.class;
	}

}
