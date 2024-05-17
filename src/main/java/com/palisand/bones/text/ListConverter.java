package com.palisand.bones.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.palisand.bones.text.TypedMarkupText.Token;

public class ListConverter implements Converter<Collection<?>> {
	private TypedMarkupText container = null;
	
	public void setContainer(TypedMarkupText text) {
		container = text;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		container.readUntilLineEnd(in);
		List<Object> result = new ArrayList<Object>();
		String newMargin = margin + TypedMarkupText.MARGIN_STEP;
		Converter<?> converter = container.getConverter(cls);
		for (Token token = container.nextToken(in); !isEnd(token,margin); token = container.nextToken(in)) {
			assert token.delimiter() == '-';
			container.consumeLastToken();
			Object value = converter.fromYaml(in, cls, newMargin);
			result.add(value);
		}
		return (Y)result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void writeYaml(Object obj, PrintWriter out, String margin) throws IOException {
		out.println();
		List<Object> list = (List<Object>)obj;
		Class<?> type = null;
		Converter<?> converter = null;
		String newMargin = margin + TypedMarkupText.MARGIN_STEP;
		String nextMargin = newMargin + TypedMarkupText.MARGIN_STEP;
		for (Object value: list) {
			out.print(newMargin);
			out.print("-");
			out.print(TypedMarkupText.MARGIN_STEP);
			if (type == null || type != value.getClass()) {
				type = value.getClass();
				converter = container.getConverter(value.getClass());
			}
			converter.writeYaml(value, out, nextMargin);
		}
	}

}
