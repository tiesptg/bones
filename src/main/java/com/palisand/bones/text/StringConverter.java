package com.palisand.bones.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class StringConverter implements Converter<String> {
	private TypedMarkupText container = null;
	
	public void setContainer(TypedMarkupText text) {
		container = text;
	}


	@SuppressWarnings("unchecked")
	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		String result = container.readUntilLineEnd(in);
		if (result.charAt(result.length()-1) == '\\') {
			StringBuilder sb = new StringBuilder(result);
			do {
				sb.replace(sb.length()-1,sb.length(),"\n");
				result = container.readUntilLineEnd(in);
				sb.append(result.substring(margin.length()));
			} while (result.charAt(result.length()-1) == '\\');
			result = sb.toString();
		}
		return (Y)result;
	}

	@Override
	public void writeYaml(Object obj, PrintWriter out, String margin) throws IOException {
		out.println(obj.toString().replace("\n", "\\\n" + margin + TypedMarkupText.MARGIN_STEP));
	}

}
