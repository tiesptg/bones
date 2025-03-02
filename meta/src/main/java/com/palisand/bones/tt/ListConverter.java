package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.Repository.Parser;
import com.palisand.bones.tt.Repository.Token;

public class ListConverter implements Converter<List<?>> {

	@Override
	public List<?> fromTypedText(Parser parser, BufferedReader in, Class<?> cls, Class<?> context, String margin) throws IOException {
		parser.readUntilLineEnd(in,false);
		// use same margin as parent for -
    margin = margin.substring(Repository.MARGIN_STEP.length());
		List<Object> result = new ArrayList<Object>();
		String newMargin = margin + Repository.MARGIN_STEP;
		Converter<?> converter = parser.getRepository().getConverter(cls);
		for (Token token = parser.nextToken(in); !isEnd(token,margin); token = parser.nextToken(in)) {
			assert token.delimiter() == '-';
			parser.consumeLastToken();
			Object value = converter.fromTypedText(parser,in, cls, context, newMargin);
			result.add(value);
		}
		return (List<?>)result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void toTypedText(Repository repository, List<?> obj, PrintWriter out, Class<?> context, String margin) throws IOException {
		out.println();
		List<Object> list = (List<Object>)obj;
		Class<Object> type = null;
		Converter<Object> converter = null;
    // use same margin as parent for -
		margin = margin.substring(Repository.MARGIN_STEP.length());
		String nextMargin = margin + Repository.MARGIN_STEP;
		for (Object value: list) {
			out.print(margin);
			out.print("-");
			out.print(Repository.MARGIN_STEP);
			if (type == null || type != value.getClass()) {
				type = (Class<Object>)value.getClass();
				converter = (Converter<Object>)repository.getConverter(value.getClass());
			}
			converter.toTypedText(repository,value, out, context, nextMargin);
		}
	}

	public Class<?> getType() {
		return List.class;
	}

  public boolean isEnd(Token token, String margin) {
    return token == null || token.margin().length() < margin.length() 
        || (token.margin().length() == margin.length() && token.delimiter() != '-');
  }
	  
  @Override
  public boolean isValueOnSameLine() {
    return false;
  }
  

}
