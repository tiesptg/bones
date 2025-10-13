package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.LinkRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;
import com.palisand.bones.tt.TextIgnore;
import com.palisand.bones.tt.Validator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Entity extends Item<MetaModel> {
  private static final RulesMap RULES =
      Rules.map().and("name", StringRules.builder().notNull(true).pattern("[A-Z]\\w+").build())
          .and("entityContainer",
              LinkRules.builder()
                  .noCycle(link -> ((ContainerRole) link.get()).getContainer().getEntityContainer())
                  .build())
          .and("idAttribute",
              LinkRules.builder().enabled(entity -> !((Entity) entity).getSuperEntity().isPresent())
                  .notNull(true).build())
          .and("superEntity", LinkRules.builder()
              .enabled(entity -> entity.getContainingAttribute() != null).build());

  @Override
  public Rules getConstraint(String field) {
    return RULES.of(field, super::getConstraint);
  }

  private final Link<Entity, Entity> superEntity =
      Link.newLink(this, ".*#/entities/.*", obj -> obj.getSpecialisations());
  private boolean abstractEntity = false;
  private final LinkList<Entity, Entity> specialisations =
      new LinkList<>(this, ".*#/entities/.*", obj -> obj.getSuperEntity());
  private List<Member> members = new ArrayList<>();
  private List<Method> methods = new ArrayList<>();
  private Link<Entity, ContainerRole> entityContainer =
      Link.newLink(this, ".*#/entities/.*/members/.*", contained -> contained.getEntity());
  private Link<Entity, Attribute> idAttribute =
      Link.newLink(this, "members/.*", attr -> attr.getIdFor());
  private LinkList<Entity, ReferenceRole> referencedFrom =
      new LinkList<>(this, ".*#/entities/.*/members/.*", refrole -> refrole.getEntity());

  public void setSuperEntity(Entity entity) throws IOException {
    superEntity.set(entity);
  }

  public void addSpecialisation(Entity entity) throws IOException {
    specialisations.add(entity);
  }

  public void removeSpecialisations(Entity entity) throws IOException {
    specialisations.remove(entity);
  }

  @TextIgnore
  public List<Attribute> getAttributes() {
    return members.stream().filter(member -> member instanceof Attribute)
        .map(member -> (Attribute) member).toList();
  }

  @TextIgnore
  public List<ReferenceRole> getReferenceRoles() {
    return members.stream().filter(member -> member instanceof ReferenceRole)
        .map(member -> (ReferenceRole) member).toList();
  }

  @TextIgnore
  public List<ContainerRole> getContainerRoles() {
    return members.stream().filter(member -> member instanceof ContainerRole)
        .map(member -> (ContainerRole) member).toList();
  }

  public Entity getEntityOfPattern(String pattern) throws IOException {
    String up = "../";
    if (pattern.startsWith(up)) {
      return getEntityContainer().get().getContainer()
          .getEntityOfPattern(pattern.substring(up.length()));
    }
    int pos = pattern.indexOf("#/");
    Entity entity = this;
    if (pos >= 0) {
      pattern = pattern.substring(pos + 2);
      entity = getContainer().getModelRootEntity();
    }
    String[] parts = pattern.split("/");
    for (int i = 0; i < parts.length; i += 2) {
      String name = parts[i];
      Optional<ContainerRole> contained =
          entity.getContainerRoles().stream().filter(c -> c.getName().equals(name)).findFirst();
      if (contained.isEmpty()) {
        return null;
      }
      entity = contained.get().getEntity().get();
    }
    return entity;
  }

  @Override
  public void doValidate(Validator validator) throws IOException {
    super.doValidate(validator);
    if (!isAbstractEntity()) {
      validator.assertNotNull("entityContainer", getEntityContainer());
    }
  }

  public String getFullname() {
    if (getContainer() != null && getContainer().getPackageName() != null) {
      return getContainer().getPackageName() + "." + getName();
    }
    return getName();
  }

  public boolean isRootEntity() throws IOException {
    return getEntityContainer().get() == null && !isAbstractEntity();
  }

}
