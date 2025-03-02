package com.palisand.bones.meta;

import java.io.IOException;
import java.util.stream.Collectors;

import com.palisand.bones.meta.generator.JavaGenerator;

public class EnumJava  extends JavaGenerator<EnumType> {

  @Override
  public void config(EnumType object) {
    setPackageAndClass(object.getContainer().getPackageName(),object.getName());
  }

  @Override
  public void generate(EnumType object) throws IOException {
    nl("package %s;", object.getContainer().getPackageName());
    nl();
    nl("public enum %s {",object.getName());
    incMargin();
    nl(object.getValues().stream().collect(Collectors.joining(",\n  ")));
    decMargin();
    nl("}");
  }

}
