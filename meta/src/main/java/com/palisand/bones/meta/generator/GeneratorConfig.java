package com.palisand.bones.meta.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class GeneratorConfig {

  private Map<Class<?>, CodeGenerator<?>[]> generators = new HashMap<>();

  public void setLogger(LogFacade logger) {
    generators.values()
        .forEach(array -> Arrays.stream(array).forEach(generator -> generator.setLogger(logger)));
  }

  protected void add(Class<?> cls, CodeGenerator<?>... generators) {
    this.generators.put(cls, generators);
  }

  @SuppressWarnings("unchecked")
  public <X> CodeGenerator<X>[] getGenerators(Class<X> cls) {
    CodeGenerator<X>[] result = (CodeGenerator<X>[]) generators.get(cls);
    if (result == null) {
      return new CodeGenerator[0];
    }
    return result;
  }
}
