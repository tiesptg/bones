package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.palisand.bones.meta.generator.JavaGenerator;
import com.palisand.bones.tt.Document;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.TextIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class EntityGenJava extends JavaGenerator<Entity> {

  @Override
  public void config(Entity entity) {
    setPackageAndClass(entity.getContainer().getPackageName(),entity.getName() + "Gen");
  }

  private void collectImports(Entity entity) throws IOException {
    addImport(Getter.class);
    addImport(Setter.class);
    addImport(NoArgsConstructor.class);
    addImport(RulesMap.class);
    addImport(Rules.class);
    if (entity.getContainerRoles().stream().anyMatch(role -> role.isMultiple())) {
      addImport(List.class);
      addImport(ArrayList.class);
    }
    if (entity.getReferenceRoles().stream().anyMatch(role -> !role.isMultiple())) {
      addImport(Link.class);
    }
    if (entity.getReferenceRoles().stream().anyMatch(role -> role.isMultiple())) {
      addImport(LinkList.class);
    }
    if (!entity.getSuperEntity().isPresent()) {
      addImport(TextIgnore.class);
      if (!entity.getEntityContainer().isPresent() && !entity.isAbstractEntity()) {
        addImport(Document.class);
      } else if (!entity.getSuperEntity().isPresent()) {
        addImport(Node.class);
      }
    }
  }
  
  private String getRuleType(Member member) throws IOException {
    switch (member.getType()) {
      case STRING: return "String";
      case BOOLEAN: return "Boolean";
      case INTEGER: case DOUBLE: return "Number";
      case ENUM: return "Enum";
      case OBJECT: {
        if (member instanceof ContainerRole role) {
          if (role.isMultiple()) {
            return "List";
          }
          return "";
        }
        if (member.isMultiple()) {
          return "List";
        }
        return "Link";
      }
    }
    throw new IOException("Member " + member + " has no supported rules type");
  }
  
  private void doRules(Member member) throws IOException {
    List<String> rules = new ArrayList<>();
    if (member.isNotNull()) {
      rules.add(".isNotNull(true)");
    }
    if (member.getEnabledWhen() != null) {
      rules.add(".enabled(object -> " + member.getEnabledWhen() + ")");
    }
    if (member instanceof ContainerRole role) {
      if (role.isMultiple()) {
        if (role.isNotEmpty()) {
          rules.add(".notEmpty(true)");
        }
      }
    } else if (member instanceof ReferenceRole role) {
      if (role.isMultiple()) {
        if (role.isNotEmpty()) {
          rules.add(".notEmpty(true)");
        }
      }
    }
    if (!rules.isEmpty()) {
      nl(".and(\"%s\",%sRules.builder()%s.build())",member.getName(),getRuleType(member),rules.stream().collect(Collectors.joining()));
    }
  }
  
  @Override
  public void generate(Entity entity) throws IOException {
    collectImports(entity);
    nl("package %s;",entity.getContainer().getPackageName());
    nl();
    printImports();
    nl();
    nl("@Getter");
    nl("@Setter");
    nl("@NoArgsConstructor");
    if (entity.getSuperEntity().isPresent()) {
      nl("public abstract class %sGen extends %s {",entity.getName(), entity.getSuperEntity().get().getName());
    } else if (entity.getEntityContainer().isPresent() || entity.isAbstractEntity()) {
      ContainerRole role = entity.getEntityContainer().get();
      if (role == null) {
        for (Link<Entity,Entity> sub: entity.getSpecialisations().getList()) {
          role = sub.get().getEntityContainer().get();
          if (role != null) {
            break;
          }
        }
      }
      nl("public abstract class %sGen extends Node<%s> {",entity.getName(), role.getContainer().getName());
    } else {
      nl("public abstract class %sGen extends Document {",entity.getName());
    }
    nl();
    incMargin();
    nl();
    margin();
    l("private static final RulesMap RULES = Rules.map()");
    for (Member member: entity.getMembers()) {
      doRules(member);
    }
    l(";\n");
    nl();
    nl("@Override");
    nl("public Rules getConstraint(String field) {");
    incMargin();
    nl("return RULES.of(field,super::getConstraint);");
    decMargin();
    nl("}");
    nl();
    for (Member member: entity.getMembers()) {
      if (member instanceof Attribute attribute) {
        nl("private %s %s = %s;",attribute.getJavaType(),attribute.getName(),attribute.getJavaDefaultValue());
      } else if (member instanceof ReferenceRole role) {
        if (role.isMultiple()) {
          if (!role.getOpposite().isPresent()) {
            nl("private final LinkList<%s,%s> %s = new LinkList<>((%s)this,\"%s\");",entity.getName()
                ,role.getEntity().get().getName(),role.getName(),entity.getName(),role.getPointerPattern());
          } else {
            nl("private final LinkList<%s,%s> %s = new LinkList<>((%s)this,\"%s\",obj -> obj.get);",entity.getName()
                ,role.getEntity().get().getName(),role.getName(),entity.getName()
                ,role.getPointerPattern(),cap(role.getOpposite().get().getName()));
          }
        } else {
          if (role.getOpposite().isPresent()) {
            nl("private final Link<%s,%s> %s = Link.newLink((%s)this,\"%s\");",entity.getName(),role.getEntity().get().getName()
                ,role.getName(),entity.getName(),role.getPointerPattern());
          } else {
            nl("private final Link<%s,%s> %s = Link.newLink<>((%s)this,\"%s\",obj -> obj.get);",entity.getName()
                ,role.getEntity().get().getName(),role.getName(),entity.getName()
                , role.getPointerPattern(),cap(role.getOpposite().get().getName()));
          }
        }
      } else if (member instanceof ContainerRole role) {
        if (role.isMultiple()) {
          nl("private List<%s> %s = new ArrayList<>();",role.getEntity().get().getName(),role.getName());
        } else {
          nl("private %s %s = null;", role.getEntity().get().getName(),role.getName());
        }
        
      }
    }
    if (!entity.getSuperEntity().isPresent()) {
      nl();
      nl("@Override");
      nl("@TextIgnore");
      nl("public String getId() {");
      incMargin();
      nl("return %s;",entity.getIdAttribute().get().getName());
      decMargin();
      nl("}");
    }
    decMargin();
    nl();
    nl("}");
  }

}
