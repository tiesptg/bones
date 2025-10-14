package com.palisand.bones.tt;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import com.palisand.bones.meta.ui.CustomEditor;

@Retention(RUNTIME)
@Target(METHOD)
public @interface Editor {
  Class<? extends CustomEditor> value();
}
