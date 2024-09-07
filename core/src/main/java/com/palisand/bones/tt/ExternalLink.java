package com.palisand.bones.tt;

import java.lang.ref.SoftReference;
import java.util.function.Function;

public class ExternalLink<C extends Node<?>,X extends Node<?>> extends Link<C,X>{

	private SoftReference<X> link = new SoftReference<>(null);
	
	public ExternalLink(C container) {
		super(container);
	}
	
	public ExternalLink(C container,Function<X,Link<X,C>> oppositeGetter) {
		super(container,oppositeGetter);
	}

	@Override
	protected void internalSet(X x) {
		link = new SoftReference<>(x);
	}

	@Override
	public X get() {
		return link.get();
	}

}

