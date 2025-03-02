package com.palisand.bones.meta;

import com.palisand.bones.meta.generator.GeneratorConfig;

public class ModelConfig extends GeneratorConfig {

  public ModelConfig() {
    add(Entity.class,new EntityGenJava(),new EntityJava());
  }
}
