package com.palisand.bones.tt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Rule {
	boolean required() default false;
	String pattern() default "";
	double min() default Double.MIN_VALUE;
	double max() default Double.MAX_VALUE;
	String before() default "";
	String after() default "";
}
