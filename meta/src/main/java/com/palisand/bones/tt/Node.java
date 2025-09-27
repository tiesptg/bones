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
  @Setter
  private String containingAttribute;

  @SuppressWarnings("unchecked")
  public void setContainer(Node<?> node, String attribute) {
    container = (N) node;
    containingAttribute = attribute;
  }

  public String getFilename() {
    return getContainingAttribute();
  }

  public void setFilename(String filename) {
    setContainingAttribute(filename);
  }

  @TextIgnore
  public abstract String getId();

  @SuppressWarnings("unchecked")
  public void beforeIdChange(String oldId, String newId) throws IOException {
    if (oldId != null) {
      ObjectConverter converter = ObjectConverter.getConverter(getClass());
      for (Property property : converter.getProperties()) {
        if (property.isLink()) {
          if (property.isList()) {
            LinkList<?, ?> list = (LinkList<?, ?>) property.get(this);
            for (Link<?, ?> link : list.getList()) {
              link.getOpposite().changeId(oldId, newId);
            }
          } else {
            Link<?, ?> link = (Link<?, ?>) property.get(this);
            if (link.isPresent()) {
              AbstractLink<?, ?> opposite = link.getOpposite();
              opposite.changeId(oldId, newId);
            }
          }
        } else if (Node.class.isAssignableFrom(property.getComponentType())) {
          if (property.isList()) {
            List<Node<?>> list = (List<Node<?>>) property.get(this);
            for (Node<?> node : list) {
              node.beforeIdChange(oldId, newId);
            }
          } else {
            Node<?> node = (Node<?>) property.get(this);
            if (node != null) {
              node.beforeIdChange(oldId, newId);
            }
          }
        }
      }
    }
  }

  public Rules getConstraint(String fieldName) {
    return null;
  }

  @SuppressWarnings("unchecked")
  public boolean validate(Validator validator) {
    validator.setNode(this);
    try {
      ObjectConverter converter = ObjectConverter.getConverter(getClass());
      for (Property property : converter.getProperties()) {
        Rules constr = (Rules) getConstraint(property.getName());
        Object value = property.getValue(this);
        if (constr != null && constr.isEnabled((N) this)) {
          constr.doValidate(validator, property.getName(), value);
        }
        if (value != null && Node.class.isAssignableFrom(property.getComponentType())
            && !property.isLink() && !property.isReadonly()) {
          if (property.isList()) {
            List<Node<?>> list = (List<Node<?>>) value;
            for (Node<?> child : list) {
              child.validate(validator);
            }
          } else {
            Node<?> node = (Node<?>) value;
            node.validate(validator);
          }
          validator.setNode(this);
        }
      }
      doValidate(validator);
    } catch (Exception ex) {
      validator.addViolation(null, ex);
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
    Node<?> root = (Node<?>) getRootContainer();
    Node<?> contextRoot = (Node<?>) context.getRootContainer();
    if (root.getContainingAttribute() == null) {
      throw new IOException(this + " should be saved before adding external links");
    }
    if (contextRoot.getContainingAttribute() == null) {
      throw new IOException(context + " should be saved before adding external links");
    }
    if (contextRoot.equals(root)) {
      return absolutePath;
    }
    return getRelativePath(contextRoot.getContainingAttribute(), root.getContainingAttribute())
        + absolutePath;
  }

  @SuppressWarnings("unchecked")
  public void delete() throws IOException {
    ObjectConverter converter = (ObjectConverter) ObjectConverter.getConverter(getClass());
    for (Property property : converter.getProperties().stream()
        .filter(property -> Node.class.isAssignableFrom(property.getComponentType())).toList()) {
      if (property.isLink()) {
        // set links to null
        if (property.isList()) {
          LinkList<?, ?> list = (LinkList<?, ?>) property.getValue(this);
          list.clear();
        } else {
          Link<?, ?> child = (Link<?, ?>) property.getValue(this);
          child.set(null);
        }
        // remove children
      } else if (property.isList()) {
        List<Node<?>> list = (List<Node<?>>) property.getValue(this);
        // to prevent changing the list while iterating
        List<Node<?>> copy = new ArrayList<Node<?>>(list);
        for (Node<?> child : copy) {
          child.delete();
        }
      } else {
        Node<?> child = (Node<?>) property.getValue(this);
        if (child != null) {
          child.delete();
        }
      }
    }
    if (getContainer() != null) {
      getContainer().removeChild(this);
    }
  }

  @SuppressWarnings("unchecked")
  void removeChild(Node<?> node) throws IOException {
    ObjectConverter converter = (ObjectConverter) ObjectConverter.getConverter(getClass());
    Property property = converter.getProperty(node.getContainingAttribute());
    if (property.isList()) {
      List<Node<?>> list = (List<Node<?>>) property.getValue(this);
      list.remove(node);
    } else {
      property.set(this, null);
    }
    node.setContainer(null, null);
  }


}
