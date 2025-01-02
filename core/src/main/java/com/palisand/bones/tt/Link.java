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
public abstract class Link<C extends Node<?>,X extends Node<?>> implements AbstractLink<C,X> {
	
	private static class Internal<C extends Node<?>,X extends Node<?>> extends Link<C,X>{

		private X link = null;
		
		public Internal(C container, String pathPattern) {
			super(container,pathPattern,null);
		}
		
		public Internal(C container,String pathPattern, Function<X,AbstractLink<X,C>> oppositeGetter) {
			super(container,pathPattern, oppositeGetter);
		}

		@Override
		public void internalSet(X x) {
			link = x;
		}
		
		@Override
		public void internalUnset(X node) throws IOException {
			if (link != null && link.equals(node)) {
				link = null;
			}
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
		
		public External(C container, String pathPattern, Function<X,AbstractLink<X,C>> oppositeGetter) {
			super(container,pathPattern, oppositeGetter);
		}

		@Override
		public void internalSet(X x) {
			link = new SoftReference<>(x);
		}
		
		@Override
		public void internalUnset(X node) throws IOException {
			if (link != null && link.get().equals(node)) {
				link = null;
			}
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
	
	public static <C extends Node<?>,X extends Node<?>>  Link<C,X> newLink(C container, String pattern,Function<X,AbstractLink<X,C>> oppositeGetter) {
		int pos = pattern.indexOf('#');
		if (pos > 0) {
			return new External<>(container,pattern,oppositeGetter);
		} 
		return new Internal<>(container,pattern,oppositeGetter);
	}
	
	private final C container;
	private final String pathPattern;
	private final Function<X,AbstractLink<X,C>> oppositeGetter;
	protected String path = null;
	private Repository repository = null;
	
	private Link(String pathPattern, C container) {
		this(container,pathPattern,null);
	}
	
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
			if (get() != null) {
				return get().toString();
			}
			return getPath();
		} catch (IOException ex) {
			throw new RuntimeException(ex); 
		}
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof Link link) {
			try {
				return getPath().equals(link.getPath());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		try {
			return getPath().hashCode();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
