package com.palisand.bones.tt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExternalLinkList<C extends Node<?>,X extends Node<?>> {

	private final List<ExternalLink<C,X>> list = new ArrayList<>();
	private final C container;
	private final String pattern;
	
	public ExternalLinkList(C container, String pattern) {
		this.container = container;
		this.pattern = pattern;
	}

	public void add(X node) throws IOException {
		ExternalLink<C,X> link = new ExternalLink<>(container,pattern);
		link.set(node);
		list.add(link);
	}
	
	public void addPath(String path) {
		ExternalLink<C,X> link = new ExternalLink<>(container,pattern);
		link.setPath(path);
		list.add(link);
	}
	
	public X get(int i) throws IOException {
		return list.get(i).get();
	}
	
	public String getPath(int i) throws IOException {
		return list.get(i).getPath();
	}
	
	public void remove(X n) throws IOException {
		for (int i = 0; i < list.size(); ++i) {
			if (get(i).equals(n)) {
				list.remove(i);
				break;
			}
		}
	}
	
	public void remove(int i) throws IOException {
		ExternalLink<C,X> link = list.remove(i);
		link.set(null);
	}

}
