package com.palisand.bones.tt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.palisand.bones.tt.ObjectConverter.Property;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Node<N extends Node<?>> {
	private static final Logger LOG = LogManager.getLogger(Node.class);
	private N container;
	@Setter private String containingAttribute;
	
	@SuppressWarnings("unchecked")
	public void setContainer(Node<?> node, String attribute) {
		container = (N)node;
		containingAttribute = attribute;
	}
	
	public abstract String getId();
	
	List<String> getPathList() {
		if (container != null) {
			List<String> result = container.getPathList();
			result.add(containingAttribute);
			result.add(getId());
			return result;
		}
		return new ArrayList<>();
	}
	
	public String getAbsolutePath() {
		return getRelativePath(null);
	}
	
	public String getRelativePath(Node<?> context) {
		List<String> pathList = getPathList();
		if (context != null) {
			List<String> contextList = context.getPathList();
			for (int i = 0; i < contextList.size() && i < pathList.size(); ++i) {
				if (!pathList.get(i).equals(contextList.get(i))) {
					if (i % 2 == 1) {
						--i;
					}
					if (i != 0) {
						pathList = pathList.subList(i, pathList.size());
						for (int x = 0; x < i; x+=2) {
							pathList.add(0, "..");
						}
					} else {
						pathList.add(0, "#");
					}
					return pathList.stream().collect(Collectors.joining("/"));
				}
			}
		}
		pathList.add(0, "#");
		return pathList.stream().collect(Collectors.joining("/"));
	}
	
	public Node<?> getRootContainer() {
		Node<?> root = this;
		while (root.container != null) {
			root = root.container;
		}
		return root;
	}
	
	@SuppressWarnings("unchecked")
	public <N extends Node<?>> N getFromPath(Mapper mapper, String path) {
		String[] pathElements = path.split("/");
		return (N)getFromPath(mapper,pathElements,0);
	}
	
	@SuppressWarnings("unchecked")
	<N extends Node<?>> N getFromPath(Mapper mapper, String[] path, int offset) {
		if (offset < path.length) {
			if (path[offset].equals("..")) {
				return getContainer().getFromPath(mapper, path, ++offset);
			}
			if (path[offset].equals("#")) {
				return getRootContainer().getFromPath(mapper,path,++offset);
			}
			ObjectConverter converter = (ObjectConverter)mapper.getConverter(getClass());
			Property property = converter.getProperties().get(path[offset]);
			Object value = null;
			try {
				value = property.getGetter().invoke(this);
				final int childOffset = offset + 1;
				if (offset == path.length) {
					return (N)value;
				}
				if (Node.class.isAssignableFrom(property.getComponentType())) {
					if (property.isList()) {
						List<Node<?>> list = (List<Node<?>>)value;
						Optional<Node<?>> node = list.stream().filter(item -> path[childOffset].equals(item.getId())).findAny();
						if (node.isPresent()) {
							return (N)node.get();
						}
						return null;
					}
					Node<?> node = (Node<?>)value;
					if (path[childOffset].equals(node.getId())) {
						return (N)node;
					}
					return null;
				}
				throw new UnsupportedOperationException("path in none node list is unsupported");
			} catch (Exception ex) {
				LOG.info("could not get value of " + path[offset] + " in " + this,ex);
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + getId();
	}

	private static String getRelativePath(String absolutePath, String absoluteContextPath) {
		Path path = Path.of(absolutePath);
		Path context = Path.of(absoluteContextPath);
		return path.relativize(context).toString();
	}
	
	public String getExternalPath(Node<?> context) throws IOException {
		String absolutePath = getAbsolutePath();
		Node<?> root = getRootContainer();
		Node<?> contextRoot = context.getRootContainer();
		if (root.getContainingAttribute() == null) {
			throw new IOException(this + " should be saved before adding external links");
		}
		if (contextRoot.getContainingAttribute() == null) {
			throw new IOException(context + " should be saved before adding external links");
		}
		return getRelativePath(root.getContainingAttribute(),contextRoot.getContainingAttribute())
				+ absolutePath;
	}
	
}
