package com.palisand.bones.tt;

import java.util.function.Function;

public class InternalLink<C extends Node<?>,X extends Node<?>> extends Link<C,X>{

	private X link = null;
	
	public InternalLink(C container) {
		super(container);
	}
	
	public InternalLink(C container,Function<X,Link<X,C>> oppositeGetter) {
		super(container,oppositeGetter);
	}

	@Override
	protected void internalSet(X x) {
		link = x;
	}

	@Override
	public X get() {
		return link;
	}

}
