package com.palisand.bones.tt;

import java.io.IOException;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class Link<C extends Node<?>,X extends Node<?>> {
	private final C container;
	private final String pathPattern;
	private final Function<X,Link<X,C>> oppositeGetter;
	protected String path = null;
	private Repository mapper = null;
	
	public Link(String pathPattern, C container) {
		this(container,pathPattern,null);
	}
	
	protected abstract void internalSet(X x);
	
	public abstract X get() throws IOException;
	
	public void set(X x) throws IOException {
		X link = get();
		if (link != null) {
			oppositeGetter.apply(link).internalSet(null);
		}
		internalSet(x);
		if (x != null) {
			oppositeGetter.apply(x).internalSet(container);
		}
	}
	
	protected abstract String getPathOfObject() throws IOException;
	
	public String getPath() throws IOException {
		if (path == null && get() != null) {
			path = getPathOfObject();
		}
		return path;
	}
}
