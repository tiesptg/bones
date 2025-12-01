package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.palisand.bones.meta.generator.JavaGenerator;
import com.palisand.bones.tt.FieldOrder;
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
    setPackageAndClass(entity.getContainer().getPackageName(), entity.getName() + "Gen");
  }

  private void collectImports(Entity entity) throws IOException {
    addImport(Getter.class);
    addImport(Setter.class);
    addImport(NoArgsConstructor.class);
    if (!getAllRules(entity, "").isEmpty()) {
      addImport(RulesMap.class);
      addImport(Rules.class);
      addImport(Rules.class.getName() + ".*");
      addImport(Node.class);
    }
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
      addImport(TextIgnore.class);
      addImport(Node.class);
    }
    if (!entity.getActiveContainer().isPresent()) {
      addImport(Node.class);
    }
  }

  @SuppressWarnings("incomplete-switch")
  private String getRuleType(Member member) throws IOException {
    switch (member.getType()) {
      case STRING:
        return "String";
      case BOOLEAN:
        return "Boolean";
      case INTEGER:
      case DOUBLE:
        return "Number";
      case ENUM:
        return "Enum";
      case OBJECT: {
        if (member instanceof ContainerRole role) {
          if (role.isMultiple()) {
            return "List";
          }
          return "";
        }
        if (member.isMultiple()) {
          return "LinkList";
        }
        return "Link";
      }
    }
    throw new IOException("Member " + member + " has no supported rules type");
  }

  private List<String> getAllRules(Entity entity, String rulesGen) throws IOException {
    List<String> rules = new ArrayList<>();
    for (Member member : entity.getMembers()) {
      String r = getRules(member, rulesGen);
      if (r != null) {
        rules.add(r);
      }
    }
    return rules;
  }

  private String getRules(Member member, String rulesGen) throws IOException {
    List<String> rules = new ArrayList<>();
    if (member.isNotNull()) {
      rules.add(".notNull(true)");
    }
    if (member.getEnabledWhen() != null) {
      rules.add(".enabled(object -> " + member.getEnabledWhen() + ")");
    }
    if (member instanceof Attribute attribute) {
      if (attribute.getType() == Type.STRING) {
        if (attribute.getMultiLine()) {
          rules.add(".multiLine(true)");
        }
      }
      if (attribute.getPattern() != null) {
        rules.add(String.format(".pattern(\"%s\")", attribute.getPattern()));
      }
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
      return String.format(".and(\"%s\",%sRules.<%sGen%s>builder()%s.build())", member.getName(),
          getRuleType(member), member.getContainer().getName(), rulesGen,
          rules.stream().collect(Collectors.joining()));
    }
    return null;
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
      for (Member member : entity.getMembers()) {
        l('"' + member.getName() + "\",");
      }
      nl("})");
    }
    String gen = "";
    String superEntity = "Node";
    String container = "<P extends Node<?>>";
    String superContainer = "<P>";
    String rulesGen = "<?>";
    if (entity.getSuperEntity().isPresent()) {
      superEntity = entity.getSuperEntity().get().getName();
    }
    if (entity.isRootEntity()) {
      container = "";
      superContainer = "<Node<?>>";
      rulesGen = "";
    }
    if (entity.getEntityContainer().isPresent()) {
      container = "";
      superContainer = '<' + entity.getEntityContainer().get().getContainer().getName() + '>';
      rulesGen = "";
    } else if (entity.getSuperEntity().isPresent()
        && entity.getSuperEntity().get().getActiveContainer().isPresent()) {
      container = "";
      superContainer = "";
      rulesGen = "";
    }
    nl("public abstract class %sGen%s extends %s%s {", entity.getName(), container, superEntity,
        superContainer);
    nl();
    incMargin();
    List<String> rules = getAllRules(entity, rulesGen);
    if (!rules.isEmpty()) {
      margin();
      l("private static final RulesMap<%sGen%s> RULES = Rules.<%sGen%s>map()", entity.getName(),
          rulesGen, entity.getName(), rulesGen);
      incMargin();
      for (String str : rules) {
        nl();
        margin();
        l(str);
      }
      l(";");
      nl();
      decMargin();
      nl();
      nl("@SuppressWarnings(\"unchecked\")");
      nl("@Override");
      nl("public Rules<? extends Node<?>> getConstraint(String field) {");
      incMargin();
      nl("return RULES.of(field,super::getConstraint);");
      decMargin();
      nl("}");
      nl();
    }
    for (Member member : entity.getMembers()) {
      if (member instanceof Attribute attribute) {
        if (attribute.getJavaDefaultValue().contains("(")) {
          nl("private %s %s;", attribute.getJavaType(), attribute.getName());
        } else {
          nl("private %s %s = %s;", attribute.getJavaType(), attribute.getName(),
              attribute.getJavaDefaultValue());
        }
      } else if (member instanceof ReferenceRole role) {
        if (role.isMultiple()) {
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
      } else if (member instanceof ContainerRole role) {
        if (role.isMultiple()) {
          nl("private List<%s> %s = new ArrayList<>();", role.getEntity().get().getName(),
              role.getName());
        } else {
          nl("private %s %s = null;", role.getEntity().get().getName(), role.getName());
        }

      }
    }
    for (Member member : entity.getMembers()) {
      if (member instanceof Attribute attribute) {
        if (attribute.getJavaDefaultValue().contains("(")) {
          nl();
          nl("public %s %s() {", attribute.getJavaType(), attribute.getName("get"));
          incMargin();
          nl("if (%s == null) {", attribute.getName());
          incMargin();
          nl("return %s;", attribute.getJavaDefaultValue());
          decMargin();
          nl("}");
          nl("return %s;", attribute.getName());
          decMargin();
          nl("}");
        }
      }
    }

    if (!entity.getSuperEntity().isPresent()) {
      nl();
      nl("@Override");
      nl("@TextIgnore");
      nl("public String getId() {");
      incMargin();
      nl("return %s;", entity.getIdAttribute().get().getName());
      decMargin();
      nl("}");
    }
    decMargin();
    nl();
    nl("}");
  }

}
