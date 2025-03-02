package com.palisand.bones.tt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
	
	public Rules getConstraint(String fieldName) {
		return null;
	}
	
	public Repository getRepository() {
	  return getRootContainer().getRepository();
	}
	
	@SuppressWarnings("unchecked")
	public boolean validate(Validator validator) {
		validator.setNode(this);
		try {
  		ObjectConverter converter = ObjectConverter.getConverter(getClass());
  		for (Property property: converter.getProperties()) {
  			Rules constr = (Rules)getConstraint(property.getName());
  			Object value = property.getValue(this);
  			if (constr != null && constr.isEnabled((N)this)) {
  				constr.doValidate(validator, property.getName(),value);
  			}
  			if (value != null && Node.class.isAssignableFrom(property.getComponentType()) && !property.isLink() && !property.isReadonly()) {
  				if (property.isList()) {
  					List<Node<?>> list = (List<Node<?>>)value;
  					for (Node<?> child: list) {
  					  child.validate(validator);
  					}
  				} else {
  					Node<?> node = (Node<?>)value;
  					node.validate(validator);
  				}
  				validator.setNode(this);
  			}
  		}
  		doValidate(validator);
		} catch (Exception ex) {
		  validator.addViolation(null,ex);
		}
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
	
	@SuppressWarnings("unchecked")
  public void delete() throws IOException {
	  ObjectConverter converter = (ObjectConverter)ObjectConverter.getConverter(getClass());
	  for (Property property: converter.getProperties().stream()
	      .filter(property -> Node.class.isAssignableFrom(property.getComponentType())).toList()) {
	    if (property.isLink()) {
	      // set links to null
	      if (property.isList()) {
	        LinkList<?,?> list = (LinkList<?,?>)property.getValue(this);
	        list.clear();
	      } else {
	        Link<?,?> child = (Link<?,?>)property.getValue(this);
	        child.set(null);
	      }
	      // remove children
	    } else if (property.isList()) {
	      List<Node<?>> list = (List<Node<?>>)property.getValue(this);
	      // to prevent changing the list while iterating
	      List<Node<?>> copy = new ArrayList<Node<?>>(list);
	      for (Node<?> child: copy) {
	        child.delete();
	      }
	    } else {
	      Node<?> child = (Node<?>)property.getValue(this);
	      if (child != null) {
	        child.delete();
	      }
	    }
	  }
    if (getContainer() != null) {
      getContainer().removeChild(this);
    } else if (this instanceof Document document){
      getRepository().removeRoot(document);
    }
	}
	
	@SuppressWarnings("unchecked")
  void removeChild(Node<?> node) throws IOException {
    ObjectConverter converter = (ObjectConverter)ObjectConverter.getConverter(getClass());
    Property property = converter.getProperty(node.getContainingAttribute());
    if (property.isList()) {
      List<Node<?>> list = (List<Node<?>>)property.getValue(this);
      list.remove(node);
    } else {
      property.set(this,null);
    }
    node.setContainer(null,null);
	}
	
	
}
