package com.palisand.bones.di;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * 
 * @author Ties Pull ter Gunne
 * 
 *         The ApplicationContext manages the dependency injection of Injectables It fully supports
 *         bidirectional or circular dependencies You register them in the ApplicationContext and
 *         get a fully injected instance through the get method It supported both type and name
 *         based registrations.
 * 
 *         To make use of this class, make a subclass of ApplicationContext and register in it your
 *         components through the register methods it requires that all managed components implement
 *         the Injectable interface
 * 
 * @see Injectable
 *
 */
public abstract class ApplicationContext {
  private final ApplicationContext parent;
  private HashMap<Class<?>, Object> readyTyped = new HashMap<>();
  private ArrayList<Object> allTyped = new ArrayList<>();

  private TreeMap<String, Object> readyNamed = new TreeMap<>();
  private TreeMap<String, Object> allNamed = new TreeMap<>();

  public ApplicationContext() {
    parent = null;
  }

  public ApplicationContext(ApplicationContext parent) {
    this.parent = parent;
  }

  /**
   * Register your managed components with this ApplicationContext You can retrieve them, with its
   * dependencies injected, through the get(Class&lt;A&gt; type) method
   * 
   * @param components: one or more components, without its managed dependencies.
   * 
   * @see ApplicationContext#get(Class)
   */
  protected void register(Injectable... components) {
    allTyped.addAll(Arrays.asList(components));
  }

  /**
   * Register your managed components with this ApplicationContext You retrieve them, with its
   * dependencies injected, through the get(String name) method
   * 
   * @param name: the name this object will be retrievable
   * @param component: the component that will be retrievable by the given name
   * 
   * @see ApplicationContext#get(String)
   */
  protected void register(String name, Injectable component) {
    if (allNamed.put(name, component) != null) {
      throw new IllegalArgumentException(
          "Injectable with name '" + name + "' is already registered");
    }
  }

  /**
   * This method returns the first component found in this ApplicationContext that can be assigned
   * to variable of type A.
   * 
   * @param <A> The type of the component you want
   * @param type The class of the component you want. It may be an interface or a class
   * @return an instance of the given type, or one of its subtypes or implementors
   * 
   * @see ApplicationContext#register(Injectable...)
   */
  @SuppressWarnings("unchecked")
  public synchronized <A> A get(Class<A> type) {
    A result = (A) readyTyped.get(type);
    if (result != null) {
      return result;
    }
    for (int i = 0; i < allTyped.size(); ++i) {
      result = (A) allTyped.get(i);
      if (type.isAssignableFrom(result.getClass())) {
        readyTyped.put(type, result);
        if (result instanceof Injectable injectable) {
          injectable.injectFrom(this);
        }
        return result;
      }
    }
    if (parent != null) {
      return parent.get(type);
    }
    throw new IllegalArgumentException(
        "no instance of class " + type.getName() + " added to the context");
  }

  /**
   * The method return the component registered with this name.
   * 
   * @param <A> The type of result of this method.
   * @param name The name of the component that you want
   * @return The component registered under the given name
   * 
   * @see ApplicationContext#register(String, Injectable)
   */
  @SuppressWarnings("unchecked")
  public synchronized <A> A get(String name) {
    A result = (A) readyNamed.get(name);
    if (result != null) {
      return result;
    }
    result = (A) allNamed.get(name);
    if (result != null) {
      readyNamed.put(name, result);
      if (result instanceof Injectable injectable) {
        injectable.injectFrom(this);
      }
    } else {
      throw new IllegalArgumentException(
          "no instance with name '" + name + "' added to the context");
    }
    return result;
  }

}
