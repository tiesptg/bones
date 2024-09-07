package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.Repository.Token;

public class ListConverter implements Converter<List<?>> {
	private Repository mapper = null;
	
	public void setMapper(Repository text) {
		mapper = text;
	}

	@Override
	public List<?> fromYaml(BufferedReader in, Class<?> cls, String margin) throws IOException {
		mapper.readUntilLineEnd(in);
		List<Object> result = new ArrayList<Object>();
		String newMargin = margin + Repository.MARGIN_STEP;
		Converter<?> converter = mapper.getConverter(cls);
		for (Token token = mapper.nextToken(in); !isEnd(token,margin); token = mapper.nextToken(in)) {
			assert token.delimiter() == '-';
			mapper.consumeLastToken();
			Object value = converter.fromYaml(in, cls, newMargin);
			result.add(value);
		}
		return (List<?>)result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void toYaml(List<?> obj, PrintWriter out, String margin) throws IOException {
		out.println();
		List<Object> list = (List<Object>)obj;
		Class<Object> type = null;
		Converter<Object> converter = null;
		String newMargin = margin + Repository.MARGIN_STEP;
		String nextMargin = newMargin + Repository.MARGIN_STEP;
		for (Object value: list) {
			out.print(newMargin);
			out.print("-");
			out.print(Repository.MARGIN_STEP);
			if (type == null || type != value.getClass()) {
				type = (Class<Object>)value.getClass();
				converter = (Converter<Object>)mapper.getConverter(value.getClass());
			}
			converter.toYaml(value, out, nextMargin);
		}
	}

	public Class<?> getType() {
		return List.class;
	}

}
