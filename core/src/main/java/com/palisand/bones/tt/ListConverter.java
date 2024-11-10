package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.Repository.Token;

public class ListConverter implements Converter<List<?>> {
	private Repository repository = null;
	
	public void setRepository(Repository text) {
		repository = text;
	}

	@Override
	public List<?> fromTypedText(BufferedReader in, Class<?> cls, String margin) throws IOException {
		repository.readUntilLineEnd(in);
		List<Object> result = new ArrayList<Object>();
		String newMargin = margin + Repository.MARGIN_STEP;
		Converter<?> converter = repository.getConverter(cls);
		for (Token token = repository.nextToken(in); !isEnd(token,margin); token = repository.nextToken(in)) {
			assert token.delimiter() == '-';
			repository.consumeLastToken();
			Object value = converter.fromTypedText(in, cls, newMargin);
			result.add(value);
		}
		return (List<?>)result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void toTypedText(List<?> obj, PrintWriter out, String margin) throws IOException {
		out.println();
		List<Object> list = (List<Object>)obj;
		Class<Object> type = null;
		Converter<Object> converter = null;
		String nextMargin = margin + Repository.MARGIN_STEP;
		for (Object value: list) {
			out.print(margin);
			out.print("-");
			out.print(Repository.MARGIN_STEP);
			if (type == null || type != value.getClass()) {
				type = (Class<Object>)value.getClass();
				converter = (Converter<Object>)repository.getConverter(value.getClass());
			}
			converter.toTypedText(value, out, nextMargin);
		}
	}

	public Class<?> getType() {
		return List.class;
	}

}
