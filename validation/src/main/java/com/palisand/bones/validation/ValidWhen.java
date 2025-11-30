package com.palisand.bones.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Predicate;

@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
public @interface ValidWhen {
  Class<? extends Predicate<?>> value();
}
