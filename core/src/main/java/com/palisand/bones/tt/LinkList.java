package com.palisand.bones.tt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class LinkList<C extends Node<?>,X extends Node<?>> {

	@Getter private final List<Link<C,X>> list = new ArrayList<>();
	private Repository repository = null;
	@Getter private final C container;
	@Getter private final String pattern;
	
	public LinkList(C container, String pattern) {
		this.container = container;
		this.pattern = pattern;
	}
	
	public void setRepository(Repository repository) {
		this.repository = repository;
		list.forEach(link -> link.setRepository(repository));
	}

	public void add(X node) throws IOException {
		Link<C,X> link = Link.newLink(container,pattern);
		link.set(node);
		link.setRepository(repository);
		list.add(link);
	}
	
	public void addPath(String path) {
		Link<C,X> link = Link.newLink(container,pattern);
		link.setPath(path);
		link.setRepository(repository);
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
		Link<C,X> link = list.remove(i);
		link.set(null);
	}

}