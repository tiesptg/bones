package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.meta.generator.JavaGenerator;

import lombok.Getter;
import lombok.Setter;

public class EntityGenJava extends JavaGenerator<Entity> {

  @Override
  public void config(Entity entity) {
    setPackageAndClass(entity.getContainer().getPackageName(),entity.getName() + "Gen");
  }

  private void collectImports(Entity entity) {
    addImport(Getter.class);
    addImport(Setter.class);
    if (entity.getContainerRoles().stream().anyMatch(role -> role.isMultiple())) {
      addImport(List.class);
      addImport(ArrayList.class);
    }
  }
  
  @Override
  public void generate(Entity entity) throws IOException {
    collectImports(entity);
    nl("package %s;",entity.getContainer().getPackageName());
    printImports();
    nl();
    nl("@Getter");
    nl("@Setter");
    nl("public class %sGen {",entity.getName());
    nl();
    incMargin();
    for (Member member: entity.getMembers()) {
      if (member instanceof Attribute attribute) {
        nl("private %s %s = %s;",attribute.getJavaType(),attribute.getName(),attribute.getJavaDefaultValue());
      } else if (member instanceof ReferenceRole role) {
        if (role.isMultiple()) {
          
        } else {
          
        }
      } else if (member instanceof ContainerRole role) {
        if (role.isMultiple()) {
          nl("private List<%s> %s = new ArrayList<>();",role.getEntity().get().getName(),role.getName());
        } else {
          nl("private %s %s = null;", role.getEntity().get().getName(),role.getName());
        }
        
      }
    }
    decMargin();
    nl();
    nl("}");
  }

}
