package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.palisand.bones.Names;
import com.palisand.bones.meta.generator.JavaGenerator;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.tt.Node;
import com.palisand.bones.validation.CamelCase;
import com.palisand.bones.validation.KebabCase;
import com.palisand.bones.validation.LowerCase;
import com.palisand.bones.validation.Max;
import com.palisand.bones.validation.Min;
import com.palisand.bones.validation.NotEmpty;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.Rules.PredicateWithException;
import com.palisand.bones.validation.SnakeCase;
import com.palisand.bones.validation.Spaced;
import com.palisand.bones.validation.UpperCase;
import com.palisand.bones.validation.ValidWhen;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class EntityGenJava extends JavaGenerator<Entity> {

  @Override
  public void config(Entity entity) {
    setPackageAndClass(entity.getContainer().getPackageName(), entity.getName() + "Gen");
  }

  private void collectImports(Entity entity) throws IOException {
    addImport(Getter.class);
    addImport(Setter.class);
    addImport(NoArgsConstructor.class);
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
    if (!entity.getMembers().isEmpty()) {
      addImport(FieldOrder.class);
    }
    if (!entity.getSuperEntity().isPresent()) {
      addImport(Node.class);
    }
    if (!entity.getActiveContainer().isPresent()) {
      addImport(Node.class);
    }
    if (entity.getIdAttribute().isPresent()) {
      addImport(IOException.class);
    }
    for (Member member : entity.getMembers()) {
      if (member instanceof Attribute attribute && !attribute.isReadOnly()) {
        if (attribute.isNotNull()) {
          addImport(NotNull.class);
        }
        if (attribute.getEnabledWhen() != null) {
          addImport(ValidWhen.class);
          addImport(PredicateWithException.class);
        }
        if (attribute.getMultiLine()) {
          addImport(MultiLine.class);
        }
        if (attribute.getMinValue() != null) {
          addImport(Min.class);
        }
        if (attribute.getMaxValue() != null) {
          addImport(Max.class);
        }
        if (attribute.getCasing() != null) {
          switch (attribute.getCasing()) {
            case UPPER_CASE:
              addImport(UpperCase.class);
              break;
            case LOWER_CASE:
              addImport(LowerCase.class);
              break;
            case SPACED:
              addImport(Spaced.class);
              break;
            case CAMEL_CASE:
            case CAMEL_CASE_WITH_CAPITAL:
              addImport(CamelCase.class);
              break;
            case SNAKE_CASE:
            case SNAKE_UPPER_CASE:
              addImport(SnakeCase.class);
              break;
            case KEBAB_CASE:
            case KEBAB_LOWER_CASE:
              addImport(KebabCase.class);
              break;
          }
        }
      } else if (member instanceof ReferenceRole role) {
        if (role.isMultiple() && role.isNotEmpty()) {
          addImport(NotEmpty.class);
        }
      } else if (member instanceof ContainerRole role) {
        if (role.isMultiple() && role.isNotEmpty()) {
          addImport(NotEmpty.class);
        }
      }
    }
  }

  private String translateToJava(Entity entity, String valid) {
    StringBuilder java = new StringBuilder(valid);
    String prefix = "object.";
    for (Member member : entity.getMembers()) {
      String name = prefix + member.getName();
      for (int i = java.indexOf(name); i != -1; i = java.indexOf(name, i)) {
        String newName = "object.get" + Names.capitalise(member.getName()) + "()";
        java.replace(i, i + name.length(), newName);
        i += 4;
      }
    }
    return java.toString();
  }

  private void printRules(Entity entity, Attribute attribute) {
    if (attribute.getEnabledWhen() != null) {
      nl();
      nl("public static class %sEnabled implements PredicateWithException<%s> {",
          Names.capitalise(attribute.getName()), entity.getName());
      incMargin();
      nl("@Override public boolean test(%s object) throws Exception {", entity.getName());
      incMargin();
      nl("return %s;", translateToJava(entity, attribute.getEnabledWhen()));
      decMargin();
      nl("}");
      decMargin();
      nl("}");
      nl();
      nl("@ValidWhen(%sEnabled.class)", Names.capitalise(attribute.getName()));
    }
    if (attribute.isNotNull()) {
      nl("@NotNull");
    }
    if (attribute.getMultiLine()) {
      nl("@MultiLine");
    }
    if (attribute.getMinValue() != null) {
      nl("@Min(%ld)", attribute.getMinValue());
    }
    if (attribute.getMaxValue() != null) {
      nl("@Max(%ld)", attribute.getMaxValue());
    }
    if (attribute.getCasing() != null) {
      switch (attribute.getCasing()) {
        case UPPER_CASE:
          nl("@UpperCase");
          break;
        case LOWER_CASE:
          nl("@LowerCase");
          break;
        case SPACED:
          nl("@Spaced");
          break;
        case CAMEL_CASE:
          nl("@CamelCase(startsWithCapitel=false)");
          break;
        case CAMEL_CASE_WITH_CAPITAL:
          nl("@CamelCase");
          break;
        case SNAKE_CASE:
          nl("@SnakeCase");
          break;
        case SNAKE_UPPER_CASE:
          nl("@SnakeCase(upperCase=true)");
          break;
        case KEBAB_CASE:
          nl("@KebabCase");
          break;
        case KEBAB_LOWER_CASE:
          nl("@KebabCase(lowerCase=true)");
          break;
      }
    }
  }

  @Override
  public void generate(Entity entity) throws IOException {
    collectImports(entity);
    nl("package %s;", entity.getContainer().getPackageName());
    nl();
    printImports();
    nl();
    nl("@Getter");
    nl("@Setter");
    nl("@NoArgsConstructor");
    if (!entity.getMembers().isEmpty()) {
      l("@FieldOrder({");
      for (Member member : entity.getAllMembers()) {
        l('"' + member.getName() + "\",");
      }
      nl("})");
    }
    String gen = "";
    String superEntity = "Node";
    String container = "<P extends Node<?>>";
    String superContainer = "<P>";
    if (entity.getSuperEntity().isPresent()) {
      superEntity = entity.getSuperEntity().get().getName();
    }
    if (entity.isRootEntity()) {
      container = "";
      superContainer = "<Node<?>>";
    }
    if (entity.getEntityContainer().isPresent()) {
      container = "";
      superContainer = '<' + entity.getEntityContainer().get().getContainer().getName() + '>';
    } else if (entity.getSuperEntity().isPresent()
        && entity.getSuperEntity().get().getActiveContainer().isPresent()) {
      container = "";
      superContainer = "";
    }
    nl("public abstract class %sGen%s extends %s%s {", entity.getName(), container, superEntity,
        superContainer);
    nl();
    incMargin();
    for (Member member : entity.getMembers()) {
      if (member instanceof Attribute attribute && !attribute.isReadOnly()) {
        printRules(entity, attribute);
        if (attribute.getDefaultValue() == null || attribute.hasDynamicDefault()) {
          nl("private %s %s;", attribute.getJavaType(), attribute.getName());
        } else {
          nl("private %s %s = %s;", attribute.getJavaType(), attribute.getName(),
              attribute.getDefaultValue());
        }
      } else if (member instanceof ReferenceRole role) {
        if (role.isMultiple()) {
          if (role.isNotEmpty()) {
            nl("@NotEmpty");
          }
          nl("private final LinkList<%s%s,%s> %s = new LinkList<>((%s%s)this,\"%s\",obj -> obj.get%s());",
              entity.getName(), gen, role.getEntity().get().getName(), role.getName(),
              entity.getName(), gen, role.getPointerPattern(),
              cap(role.getOpposite().get().getName()));
        } else {
          nl("private final Link<%s%s,%s> %s = Link.newLink((%s%s)this,\"%s\",obj -> obj.get%s());",
              entity.getName(), gen, role.getEntity().get().getName(), role.getName(),
              entity.getName(), gen, role.getPointerPattern(),
              cap(role.getOpposite().get().getName()));
        }
      } else if (member instanceof ContainerRole role && !member.isReadOnly()) {
        if (role.isMultiple()) {
          if (role.isNotEmpty()) {
            nl("@NotEmpty");
          }
          nl("private List<%s> %s = new ArrayList<>();", role.getEntity().get().getName(),
              role.getName());
        } else {
          nl("private %s %s = null;", role.getEntity().get().getName(), role.getName());
        }

      }
    }
    for (Member member : entity.getMembers()) {
      if (member instanceof Attribute attribute) {
        if (attribute.isReadOnly()) {
          nl();
          nl("public %s %s() {", attribute.getJavaType(), attribute.getName("get"));
          incMargin();
          nl("return %s;", attribute.getDefaultValue());
          decMargin();
          nl("}");
        } else if (attribute.hasDynamicDefault()) {
          nl();
          nl("public %s %s() {", attribute.getJavaType(), attribute.getName("get"));
          incMargin();
          nl("if (%s == %s) {", attribute.getName(), attribute.getJavaTypeDefault());
          incMargin();
          nl("return %s;", attribute.getDefaultValue());
          decMargin();
          nl("}");
          nl("return %s;", attribute.getName());
          decMargin();
          nl("}");
        }
      }
    }

    if (entity.getIdAttribute().isPresent()) {
      Attribute attribute = entity.getIdAttribute().get();
      nl();
      nl("@Override");
      nl("public String getId() {");
      incMargin();
      nl("return %s;", attribute.getName());
      decMargin();
      nl("}");
      nl();
      nl("public void %s(%s value) throws IOException {", attribute.getName("set"),
          attribute.getJavaType());
      incMargin();
      nl("beforeIdChange(this.%s, value);", attribute.getName());
      nl("%s = value;", attribute.getName());
      decMargin();
      nl("}");
    }
    decMargin();
    nl();
    nl("}");
  }

}
