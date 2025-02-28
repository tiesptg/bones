package com.palisand.meta.maven;

import java.util.HashMap;
import java.util.Map;

public abstract class GeneratorConfig {
  
  private Map<Class<?>,CodeGenerator<?>[]> generators = new HashMap<>();

  protected void add(Class<?> cls, CodeGenerator<?>...generators) {
    this.generators.put(cls,generators);
  }
  
  @SuppressWarnings("unchecked")
  public <X> CodeGenerator<X>[] getGenerators(Class<X> cls) {
    CodeGenerator<X>[] result = (CodeGenerator<X>[])generators.get(cls);
    if (result == null) {
      return new CodeGenerator[0];
    }
    return result;
  }
}
