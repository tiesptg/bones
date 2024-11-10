package com.palisand.bones.tt;

import java.io.IOException;
import java.util.function.Function;

public class InternalLink<C extends Node<?>,X extends Node<?>> extends Link<C,X>{

	private X link = null;
	
	public InternalLink(C container, String pathPattern) {
		super(container,pathPattern,null);
	}
	
	public InternalLink(C container,String pathPattern, Function<X,Link<X,C>> oppositeGetter) {
		super(container,pathPattern, oppositeGetter);
	}

	@Override
	protected void internalSet(X x) {
		link = x;
	}

	@Override
	public X get() throws IOException {
		if (link == null && path != null) {
			link = getRepository().getFromPath(getContainer(), path);
		}
		return link;
	}
	
	public boolean isAbsolute() {
		return getPathPattern().contains("#");
	}

	@Override
	protected String getPathOfObject() {
		if (link != null) {
			if (isAbsolute()) {
				return link.getAbsolutePath();
			}
			return link.getRelativePath(getContainer());
		}
		return null;
	}

}
