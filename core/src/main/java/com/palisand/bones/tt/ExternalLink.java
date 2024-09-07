package com.palisand.bones.tt;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.function.Function;

public class ExternalLink<C extends Node<?>,X extends Node<?>> extends Link<C,X>{

	private SoftReference<X> link = new SoftReference<>(null);
	
	public ExternalLink(C container,String pathPattern) {
		super(pathPattern,container);
	}
	
	public ExternalLink(C container, String pathPattern, Function<X,Link<X,C>> oppositeGetter) {
		super(container,pathPattern, oppositeGetter);
	}

	@Override
	protected void internalSet(X x) {
		link = new SoftReference<>(x);
	}

	@Override
	public X get() throws IOException {
		if ((link == null || link.get() == null) && path != null) {
			internalSet(getMapper().getFromPath(getContainer(), path));
		}
		return link != null ? link.get() : null;
	}

	@Override
	protected String getPathOfObject() throws IOException {
		if (link != null && link.get() != null) {
			return link.get().getExternalPath(getContainer());
		}
		return null;
	}

}

