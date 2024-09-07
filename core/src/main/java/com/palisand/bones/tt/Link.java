package com.palisand.bones.tt;

import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class Link<C extends Node<?>,X extends Node<?>> {
	private final C container;
	private final Function<X,Link<X,C>> oppositeGetter;
	public String path = null;
	
	public Link(C container) {
		this(container,null);
	}
	
	protected abstract void internalSet(X x);
	
	public abstract X get();
	
	public void set(X x) {
		X link = get();
		if (link != null) {
			oppositeGetter.apply(link).set(null);
		}
		internalSet(x);
		if (x != null) {
			oppositeGetter.apply(x).set(container);
		}
	}	
}
