package com.palisand.bones.tt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import lombok.Getter;
import lombok.Setter;

public class LinkList<C extends Node<?>,X extends Node<?>> implements AbstractLink<C,X> {

	@Getter private final List<Link<C,X>> list = new ArrayList<>();
	private Repository repository = null;
	@Getter private final C container;
	@Getter private final String pattern;
	private final Function<X,AbstractLink<X,C>> oppositeGetter;
	
	public LinkList(C container, String pattern, Function<X,AbstractLink<X,C>> oppositeGetter) {
		this.container = container;
		this.pattern = pattern;
		this.oppositeGetter = oppositeGetter;
	}
	
	public void setRepository(Repository repository) {
		this.repository = repository;
		list.forEach(link -> link.setRepository(repository));
	}

	public void add(X node) throws IOException {
		add(node,true);
	}
	
	private void add(X node, boolean doOpposite) throws IOException {
		Link<C,X> link = Link.newLink(container,pattern,oppositeGetter);
		if (doOpposite) {
			link.set(node);
		} else {
			link.internalSet(node);
		}
		link.setRepository(repository);
		list.add(link);
		
	}
	
	public void addPath(String path) throws IOException {
		addPath(path,true);
	}
	
	private void addPath(String path, boolean doOpposite) throws IOException {
		Link<C,X> link = Link.newLink(container,pattern,oppositeGetter);
		link.setPath(path);
		link.setRepository(repository);
		if (doOpposite) {
			link.set(link.get());
		}
		list.add(link);
	}
	
	public X get(int i) throws IOException {
		return list.get(i).get();
	}
	
	public String getPath(int i) throws IOException {
		return list.get(i).getPath();
	}
	
	public void remove(X n) throws IOException {
		remove(n,true);
	}
	
	private void remove(X n, boolean doOpposite) throws IOException {
		for (int i = 0; i < list.size(); ++i) {
			if (get(i).equals(n)) {
				Link<C,X> link = list.remove(i);
				if (doOpposite) {
					link.set(null);
				}
				break;
			}
		}
	}
	
	public void remove(int i) throws IOException {
		Link<C,X> link = list.remove(i);
		link.set(null);
	}

	@Override
	public void internalSet(X node) throws IOException {
		add(node,false);
	}

	@Override
	public void internalUnset(X node) throws IOException {
		remove(node,false);
	}

}
