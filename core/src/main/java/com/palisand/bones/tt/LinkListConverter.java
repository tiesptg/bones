package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class LinkListConverter implements Converter<List<String>> {
	private ListConverter listConverter = null;
	
	@Override
	public void setRepository(Repository text) {
		listConverter = (ListConverter)text.getConverter(List.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> fromTypedText(BufferedReader in, Class<?> cls, String margin) throws IOException {
		return (List<String>)listConverter.fromTypedText(in, String.class, margin);
	}
	
	@Override
	public void toTypedText(List<String> list, PrintWriter out, String margin) throws IOException {
		listConverter.toTypedText(list, out, margin);
	}

	@Override
	public Class<?> getType() {
		return LinkList.class;
	}

}
