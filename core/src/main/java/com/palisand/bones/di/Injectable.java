package com.palisand.bones.di;

/**
 * Implement this interface when you want the dependencies of your components injected
 * 
 * The injectFrom method should look something like this:
 * {@code
 * public void injectFrom(ApplicationContext ctx) {
 * 		dependency1 = ctx.get(TypeOfDependency.class);
 * 		dependency2 = ctx.get("NameOfComponent");
 * }
 * }
 * 
 * 
 * @author Ties Pull ter Gunne
 *
 */
public interface Injectable {
	
	/**
	 * implement this method in your component with assignments of your dependencies
	 * by calling a get method of ApplicationContext
	 * If your component needs more initialization after dependency injection 
	 * you can also do that at the end of this method
	 * 
	 * @param context the provided ApplicationContext
	 * 
	 * @see ApplicationContext
	 */
	void injectFrom(ApplicationContext context);
}
