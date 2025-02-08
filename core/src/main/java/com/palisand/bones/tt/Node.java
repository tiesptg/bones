package com.palisand.bones.tt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.palisand.bones.tt.ObjectConverter.Property;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Node<N extends Node<?>> {
	private N container;
	@Setter private String containingAttribute;
	
	@SuppressWarnings("unchecked")
	public void setContainer(Node<?> node, String attribute) {
		container = (N)node;
		containingAttribute = attribute;
	}
	
	@TextIgnore
	public abstract String getId();
	
	public <M extends Node<?>> Rules<M> getConstraint(String fieldName) {
		return null;
	}
	
	public Repository getRepository() {
	  return getRootContainer().getRepository();
	}
	
	@SuppressWarnings("unchecked")
	public boolean validate(Validator validator) {
		validator.setNode(this);
		ObjectConverter converter = ObjectConverter.getConverter(getClass());
		for (Property property: converter.getProperties()) {
			Rules<N> constr = (Rules<N>)getConstraint(property.getName());
			Object value = property.getValue(this);
			if (constr != null && constr.isEnabled((N)this)) {
				constr.doValidate(validator, property.getName(),value);
			}
			if (value != null && Node.class.isAssignableFrom(property.getComponentType()) && !property.isLink() && !property.isReadonly()) {
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
		return validator.containsErrors();
	}
	
	protected void doValidate(Validator validator) {
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
	
	public Document getRootContainer() {
		Node<?> root = this;
		while (root.container != null) {
			root = root.container;
		}
		return (Document)root;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + getId();
	}

	private static String getRelativePath(String absoluteContextPath, String absolutePath) {
		Path context = Path.of(absoluteContextPath).getParent();
		Path path = Path.of(absolutePath);
		return context.relativize(path).toString();
	}
	
	public String getAbsolutePath() {
	  List<String> parts = getPathList();
	  return "#/" + parts.stream().collect(Collectors.joining("/"));
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
		if (contextRoot.equals(root)) {
		  return absolutePath;
		}
	  return getRelativePath(contextRoot.getContainingAttribute(),root.getContainingAttribute())
				+ absolutePath;
	}
	
	
}
