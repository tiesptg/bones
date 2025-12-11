package com.palisand.bones.tt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.palisand.bones.Classes.Property;
import com.palisand.bones.tt.ObjectConverter.EditorProperty;
import com.palisand.bones.validation.Rules.Violation;
import com.palisand.bones.validation.Validatable;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Node<N extends Node<?>> implements Validatable {
  @TextIgnore private N container;
  @Setter
  @TextIgnore private String containingAttribute;

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

  public abstract String getId();

  @SuppressWarnings("unchecked")
  public void beforeIdChange(String oldId, String newId) throws IOException {
    if (oldId != null) {
      ObjectConverter converter = ObjectConverter.getConverter(getClass());
      for (EditorProperty<?> property : converter.getProperties()) {
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

  protected Property<?> getProperty(List<Property<?>> properties, String name) {
    for (Property<?> property : properties) {
      if (property.getField().getName().equals(name)) {
        return property;
      }
    }
    return null;
  }

  @Override
  public void doValidate(List<Violation> result, List<Property<?>> properties) throws Exception {

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
    List<String> list = new ArrayList<>();
    Node<?> node = this;
    while (node != node.getRootContainer()) {
      list.add(node.getId());
      node = node.getContainer();
    }
    if (list.isEmpty()) {
      list.add(getId());
    } else {
      Collections.reverse(list);
    }
    return getClass().getSimpleName() + ":" + list.stream().collect(Collectors.joining("."));
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
    return getRelativePath(contextRoot.getContainingAttribute(), root.getContainingAttribute())
        + absolutePath;
  }

  @SuppressWarnings("unchecked")
  public void delete() throws IOException {
    ObjectConverter converter = ObjectConverter.getConverter(getClass());
    for (EditorProperty<?> property : converter.getProperties().stream()
        .filter(property -> Node.class.isAssignableFrom(property.getComponentType())).toList()) {
      if (property.isLink()) {
        // set links to null
        if (property.isList()) {
          LinkList<?, ?> list = (LinkList<?, ?>) property.get(this);
          list.clear();
        } else {
          Link<?, ?> child = (Link<?, ?>) property.get(this);
          child.set(null);
        }
        // remove children
      } else if (property.isList()) {
        List<Node<?>> list = (List<Node<?>>) property.get(this);
        // to prevent changing the list while iterating
        List<Node<?>> copy = new ArrayList<Node<?>>(list);
        for (Node<?> child : copy) {
          child.delete();
        }
      } else {
        Node<?> child = (Node<?>) property.get(this);
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
    ObjectConverter converter = ObjectConverter.getConverter(getClass());
    EditorProperty<?> property = converter.getProperty(node.getContainingAttribute());
    if (property.isList()) {
      List<Node<?>> list = (List<Node<?>>) property.get(this);
      list.remove(node);
    } else {
      property.set(this, null);
    }
    node.setContainer(null, null);
  }

}
