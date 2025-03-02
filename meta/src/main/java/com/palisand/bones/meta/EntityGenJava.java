package com.palisand.bones.meta;

import com.palisand.bones.meta.generator.JavaGenerator;

public class EntityGenJava extends JavaGenerator<Entity> {

  @Override
  public void config(Entity entity) {
    setPackageAndClass(entity.getContainer().getPackageName(),entity.getName() + "Gen");
  }

  private void collectImports(Entity entity) {
    
  }
  
  @Override
  public void generate(Entity entity) {
    collectImports(entity);
    nl("package %s;",entity.getContainer().getPackageName());
    printImports();
    nl("public class %sGen {",entity.getName());
    nl("}");
  }

}
