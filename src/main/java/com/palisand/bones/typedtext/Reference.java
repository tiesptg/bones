package com.palisand.bones.typedtext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Reference {

	/** regular expression matching the allowed paths for this path
	 */
	String pathPattern();
	
	/** name of the field that holds the opposite role of this association
	 */
	String opposite() default "";

}
