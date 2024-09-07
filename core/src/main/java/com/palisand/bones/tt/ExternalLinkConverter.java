package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import lombok.Setter;

public class ExternalLinkConverter implements Converter<ExternalLink<Node<?>>> {
	@Setter private Mapper mapper;
	@Setter private Node<?> context;

	@Override
	public Class<?> getType() {
		return ExternalLinkConverter.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ExternalLink<Node<?>> fromYaml(BufferedReader in, Class<?> cls, String margin) throws IOException {
		StringConverter converter = (StringConverter)mapper.getConverter(String.class);
		String path = (String)converter.fromYaml(in, cls, margin);
		ExternalLink<Node<?>> link = new ExternalLink<Node<?>>(context);
		link.setPath(path);
		return link;
	}

	@Override
	public void toYaml(ExternalLink<Node<?>> obj, PrintWriter out, String margin) throws IOException {
		StringConverter converter = (StringConverter)mapper.getConverter(String.class);
		converter.toYaml(obj.getPath(), out, margin);
	}

}
