package com.palisand.bones.tt;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class Link<C extends Node<?>,X extends Node<?>> {
	
	private static class Internal<C extends Node<?>,X extends Node<?>> extends Link<C,X>{

		private X link = null;
		
		public Internal(C container, String pathPattern) {
			super(container,pathPattern,null);
		}
		
		public Internal(C container,String pathPattern, Function<X,Link<X,C>> oppositeGetter) {
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
	private static class External<C extends Node<?>,X extends Node<?>> extends Link<C,X>{

		private SoftReference<X> link = new SoftReference<>(null);
		
		public External(C container,String pathPattern) {
			super(pathPattern,container);
		}
		
		public External(C container, String pathPattern, Function<X,Link<X,C>> oppositeGetter) {
			super(container,pathPattern, oppositeGetter);
		}

		@Override
		protected void internalSet(X x) {
			link = new SoftReference<>(x);
		}

		@Override
		public X get() throws IOException {
			if ((link == null || link.get() == null) && path != null) {
				internalSet(getRepository().getFromPath(getContainer(), path));
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
	
	public static <C extends Node<?>,X extends Node<?>>  Link<C,X> newLink(C container, String pattern) {
		int pos = pattern.indexOf('#');
		if (pos > 0) {
			return new External<>(container,pattern);
		} 
		return new Internal<>(container,pattern);
	}
	
	public static <C extends Node<?>,X extends Node<?>>  Link<C,X> newLink(C container, String pattern,Function<X,Link<X,C>> oppositeGetter) {
		int pos = pattern.indexOf('#');
		if (pos > 0) {
			return new External<>(container,pattern,oppositeGetter);
		} 
		return new Internal<>(container,pattern,oppositeGetter);
	}
	
	private final C container;
	private final String pathPattern;
	private final Function<X,Link<X,C>> oppositeGetter;
	protected String path = null;
	private Repository repository = null;
	
	private Link(String pathPattern, C container) {
		this(container,pathPattern,null);
	}
	
	protected abstract void internalSet(X x);
	
	public abstract X get() throws IOException;
	
	public void set(X x) throws IOException {
		X link = get();
		if (oppositeGetter != null && link != null) {
			oppositeGetter.apply(link).internalSet(null);
		}
		internalSet(x);
		if (oppositeGetter != null && x != null) {
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
	
	public String toString() {
		try {
			return getPath();
		} catch (IOException ex) {
			return "<Error in path>";
		}
	}
}
