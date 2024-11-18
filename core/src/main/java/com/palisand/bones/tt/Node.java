package com.palisand.bones.tt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.palisand.bones.tt.ObjectConverter.Property;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Node<N extends Node<?>> {
	private N container;
	@Setter private String containingAttribute;
	private static final Map<String,ObjectConverter> CONVERTERS = new TreeMap<>();
	
	public static ObjectConverter getConverter(Class<?> c) {
		ObjectConverter result = CONVERTERS.get(c.getName());
		if (result == null) {
			result = new ObjectConverter(c);
			CONVERTERS.put(c.getName(), result);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public void setContainer(Node<?> node, String attribute) {
		container = (N)node;
		containingAttribute = attribute;
	}
	
	public abstract String getId();
	
	public <M extends Node<?>> PropertyConstraint<M> getConstraint(String fieldName) {
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public boolean validate(Validator validator) {
		validator.setNode(this);
		ObjectConverter converter = getConverter(getClass());
		for (Property property: converter.getProperties()) {
			PropertyConstraint<N> constr = (PropertyConstraint<N>)getConstraint(property.getName());
			Object value = property.getValue(this);
			if (constr != null && constr.isEnabled((N)this)) {
				constr.doValidate(validator, property.getName(),value);
			}
			if (value != null && Node.class.isAssignableFrom(property.getComponentType()) && !property.isLink()) {
				if (property.isList()) {
					List<Node<?>> list = (List<Node<?>>)value;
					list.forEach(node -> node.validate(validator));
				} else {
					Node<?> node = (Node<?>)value;
					node.validate(validator);
				}
				validator.setNode(this);
			}
		}
		doValidate(validator);
		return validator.getViolations().isEmpty();
	}
	
	public void doValidate(Validator validator) {
		// overrideable method to implement custom validations
	}
	
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
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + getId();
	}

	private static String getRelativePath(String absoluteContextPath, String asbsolutePath) {
		Path context = Path.of(absoluteContextPath).getParent();
		Path path = Path.of(asbsolutePath);
		return context.relativize(path).toString();
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
		return getRelativePath(contextRoot.getContainingAttribute(),root.getContainingAttribute())
				+ absolutePath;
	}
	
	
}
