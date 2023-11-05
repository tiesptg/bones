package com.palisand.bones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * 
 * @author Ties Pull ter Gunne
 * 
 * The ApplicationContext manages the dependency injection of Injectables
 * It fully supports bidirectional or circular dependencies
 * You register them in the ApplicationContext and get a fully injected instance through the get method
 * It supported both type and name based registrations.
 * 
 * To make use of this class, make a subclass of ApplicationContext and register in it your components through the register methods
 * it requires that all managed components implement the Injectable interface
 * 
 * @see Injectable
 *
 */
public abstract class ApplicationContext {
	private HashMap<Class<?>,Injectable> readyTyped = new HashMap<>();
	private ArrayList<Injectable> allTyped = new ArrayList<>();
	
	private TreeMap<String,Injectable> readyNamed = new TreeMap<>();
	private TreeMap<String,Injectable> allNamed = new TreeMap<>();
	
	/**
	 * Register your managed components with this ApplicationContext
	 * You can retrieve them, with its dependencies injected, through the get(Class<A> type) method
	 * 
	 * @param components: one or more components, without its managed dependencies.
	 * 
	 * @see ApplicationContext#get(Class)
	 */
	protected void register(Injectable...components) {
		allTyped.addAll(Arrays.asList(components));
	}
	
	/**
	 * Register your managed components with this ApplicationContext
	 * You retrieve them, with its dependencies injected, through the get(String name) method
	 * 
	 * @param name: the name this object will be retrievable
	 * @param component: the component that will be retrievable by the given name
	 * 
	 * @throws IllegalArgumentException: when the name is already registered
	 * 
	 * @see ApplicationContext#get(String)
	 */
	protected void register(String name, Injectable component) {
		if (allNamed.put(name,component) != null) {
			throw new IllegalArgumentException("Injectable with name '" + name + "' is already registered");
		}
	}
	
	/**
	 * This method returns the first component found in this ApplicationContext that can be assigned to variable of type A.
	 * 
	 * @param <A> The type of the component you want
	 * @param type The class of the component you want. It may be an interface or a class
	 * @return an instance of the given type, or one of its subtypes or implementors
	 * 
	 * @throws IllegalArgumentException: when no component of this type is registered
	 * 
	 * @see ApplicationContext#register(Injectable...)
	 */
	@SuppressWarnings("unchecked")
	public synchronized <A extends Injectable> A get(Class<A> type) {
		A result = (A)readyTyped.get(type);
		if (result != null) {
			return result;
		}
		for (int i = 0; i < allTyped.size(); ++i) {
			result = (A)allTyped.get(i);
			if (type.isAssignableFrom(result.getClass())) {
				readyTyped.put(type,result);
				result.injectFrom(this);
				return result;
			}
		}
		throw new IllegalArgumentException("no instance of class " + type.getName() + " added to the context");
	}
	
	/**
	 * The method return the component registered with this name.
	 * 
	 * @param <A> The type of result of this method.
	 * @param name The name of the component that you want
	 * @return The component registered under the given name
	 * 
	 * @throws IllegalArgumentException when no component with this name can be found
	 * 
	 * @see ApplicationContext#register(String, Injectable)
	 */
	@SuppressWarnings("unchecked")
	public synchronized <A extends Injectable> A get(String name) {
		A result = (A)readyNamed.get(name);
		if (result != null) {
			return result;
		}
		result = (A)allNamed.get(name);
		if (result != null) {
			readyNamed.put(name, result);
			result.injectFrom(this);
		} else {
			throw new IllegalArgumentException("no instance with name '" + name + "' added to the context");
		}
		return result;
	}

}
