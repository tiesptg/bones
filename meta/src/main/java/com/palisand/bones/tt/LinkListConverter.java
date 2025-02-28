package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.palisand.bones.tt.Repository.Parser;

public class LinkListConverter implements Converter<List<String>> {
	private ListConverter listConverter = null;
	
	public void init(Repository repository) {
	  listConverter = (ListConverter)repository.getConverter(List.class);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> fromTypedText(Parser parser, BufferedReader in, Class<?> cls, Class<?> context, String margin) throws IOException {
		return (List<String>)listConverter.fromTypedText(parser, in, String.class, context, margin);
	}
	
	@Override
	public void toTypedText(Repository repository, List<String> list, PrintWriter out, Class<?> context, String margin) throws IOException {
		listConverter.toTypedText(repository,list, out, context, margin);
	}

	@Override
	public Class<?> getType() {
		return LinkList.class;
	}

}
