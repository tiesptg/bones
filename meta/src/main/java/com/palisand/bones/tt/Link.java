package com.palisand.bones.tt;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class Link<C extends Node<?>, X extends Node<?>> implements AbstractLink<C, X> {

  private static class Internal<C extends Node<?>, X extends Node<?>> extends Link<C, X> {

    private X link = null;

    public Internal(C container, String pathPattern,
        Function<X, AbstractLink<X, C>> oppositeGetter) {
      super(container, pathPattern, oppositeGetter);
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
        link = Repository.getInstance().getFromPath(getContainer(), path);
      }
      return link;
    }

  }
  private static class External<C extends Node<?>, X extends Node<?>> extends Link<C, X> {

    private SoftReference<X> link = new SoftReference<>(null);

    public External(C container, String pathPattern,
        Function<X, AbstractLink<X, C>> oppositeGetter) {
      super(container, pathPattern, oppositeGetter);
    }

    @Override
    public void internalSet(X x) {
      if (x != null) {
        link = new SoftReference<>(x);
      } else {
        link = null;
      }
      path = null;
    }

    @Override
    public void internalUnset(X node) throws IOException {
      if (link != null && link.get() != null && link.get().equals(node)) {
        link = null;
        path = null;
      }
    }

    @Override
    public X get() throws IOException {
      if ((link == null || link.get() == null) && path != null) {
        internalSet(Repository.getInstance().getFromPath(getContainer(), path));
      }
      return link != null ? link.get() : null;
    }

  }

  public static <C extends Node<?>, X extends Node<?>> Link<C, X> newLink(C container,
      String pattern, Function<X, AbstractLink<X, C>> oppositeGetter) {
    int pos = pattern.indexOf('#');
    if (pos > 0) {
      return new External<>(container, pattern, oppositeGetter);
    }
    return new Internal<>(container, pattern, oppositeGetter);
  }

  private final C container;
  private final String pathPattern;
  private final Function<X, AbstractLink<X, C>> oppositeGetter;
  protected String path = null;

  public boolean isPresent() throws IOException {
    return get() != null;
  }

  public abstract X get() throws IOException;

  public void set(X x) throws IOException {
    X link = get();
    if (oppositeGetter != null && link != null) {
      oppositeGetter.apply(link).internalUnset(container);
    }
    internalSet(x);
    if (oppositeGetter != null && x != null) {
      oppositeGetter.apply(x).internalSet(container);
    }
  }

  private static String getRelativePath(String absolutePath, String absoluteContextPath) {
    Path context = Path.of(absoluteContextPath).getParent();
    Path target = Path.of(absolutePath);
    return context.relativize(target).toString();
  }

  protected String getPathOfObject(X node) throws IOException {
    String pattern = getPathPattern();
    int index = pattern.indexOf('#');
    String file = null;
    if (index != -1) {
      file = pattern.substring(0, index);
      pattern = pattern.substring(index);

      if (node.getRootContainer().equals(getContainer().getRootContainer())) {
        file = null;
      } else {
        file = getRelativePath(node.getRootContainer().getFilename(),
            getContainer().getRootContainer().getFilename());
      }
    }
    String[] parts = pattern.split("/");
    int i = parts.length - 1;
    Node<?> n = node;
    while (i > 0 && !parts[i].equals("..")) {
      parts[i] = n.getId();
      n = n.getContainer();
      i -= 2;
    }

    String result = Arrays.stream(parts).collect(Collectors.joining("/"));
    if (file != null) {
      result = file + result;
    }
    return result;
  }

  public String getPath() throws IOException {
    if (path == null) {
      X node = get();
      if (node != null) {
        setPath(getPathOfObject(node));
      }
    }
    return path;
  }

  public void setPath(String path) {
    if (!path.matches(pathPattern)) {
      System.err.println("Path " + path + " does not comply with pattern " + pathPattern);
    }
    this.path = path;
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

  public AbstractLink<X, C> getOpposite() throws IOException {
    return getOppositeGetter().apply(get());
  }

  public void changeId(String oldId, String newId) {
    if (path != null) {
      String[] parts = path.split("/");
      for (int i = 0; i < parts.length; ++i) {
        if (parts[i].equals(oldId)) {
          parts[i] = newId;
          break;
        }
      }
      setPath(Arrays.stream(parts).collect(Collectors.joining("/")));
    }
  }

}
