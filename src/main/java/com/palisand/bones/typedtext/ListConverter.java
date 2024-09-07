package com.palisand.bones.typedtext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.palisand.bones.typedtext.Mapper.Token;

public class ListConverter implements Converter<Collection<?>> {
	private Mapper mapper = null;
	
	public void setMapper(Mapper text) {
		mapper = text;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Y> Y fromYaml(BufferedReader in, Class<Y> cls, String margin) throws IOException {
		mapper.readUntilLineEnd(in);
		List<Object> result = new ArrayList<Object>();
		String newMargin = margin + Mapper.MARGIN_STEP;
		Converter<?> converter = mapper.getConverter(cls);
		for (Token token = mapper.nextToken(in); !isEnd(token,margin); token = mapper.nextToken(in)) {
			assert token.delimiter() == '-';
			mapper.consumeLastToken();
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
		String newMargin = margin + Mapper.MARGIN_STEP;
		String nextMargin = newMargin + Mapper.MARGIN_STEP;
		for (Object value: list) {
			out.print(newMargin);
			out.print("-");
			out.print(Mapper.MARGIN_STEP);
			if (type == null || type != value.getClass()) {
				type = value.getClass();
				converter = mapper.getConverter(value.getClass());
			}
			converter.writeYaml(value, out, nextMargin);
		}
	}

	public Class<?> getType() {
		return List.class;
	}

}
