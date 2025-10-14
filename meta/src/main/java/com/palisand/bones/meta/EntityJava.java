package com.palisand.bones.meta;

import java.io.IOException;
import com.palisand.bones.meta.generator.JavaGenerator;
import com.palisand.bones.tt.Node;
import lombok.NoArgsConstructor;

public class EntityJava extends JavaGenerator<Entity> {

  @Override
  public void config(Entity entity) {
    setPackageAndClass(entity.getContainer().getPackageName(), entity.getName());
    setManualEditingAllowed(true);
  }

  private void collectImports(Entity entity) throws IOException {
    addImport(NoArgsConstructor.class);
    ContainerRole role = entity.getEntityContainer().get();
    if (role == null && !entity.isRootEntity()) {
      addImport(Node.class);
    }
  }

  @Override
  public void generate(Entity entity) throws IOException {
    collectImports(entity);
    nl("package %s;", entity.getContainer().getPackageName());
    printImports();
    nl();
    nl("@NoArgsConstructor");
    ContainerRole role = entity.getEntityContainer().get();
    if (role == null && !entity.isRootEntity()) {
      nl("public class %s<C extends Node<?>> extends %sGen<C> {", entity.getName(),
          entity.getName());
    } else {
      nl("public class %s extends %sGen {", entity.getName(), entity.getName());
    }
    nl();
    nl("}");
  }

}
