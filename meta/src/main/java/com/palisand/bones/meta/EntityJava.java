package com.palisand.bones.meta;

import com.palisand.bones.meta.generator.JavaGenerator;

public class EntityJava extends JavaGenerator<Entity> {

  @Override
  public void config(Entity entity) {
    setPackageAndClass(entity.getContainer().getPackageName(),entity.getName());
    setManualEditingAllowed(true);
  }

  private void collectImports(Entity entity) {
    
  }
  
  @Override
  public void generate(Entity entity) {
    collectImports(entity);
    nl("package %s;",entity.getContainer().getPackageName());
    printImports();
    nl();
    nl("public class %s extends %sGen {",entity.getName(),entity.getName());
    nl();
    nl("}");
  }

}
